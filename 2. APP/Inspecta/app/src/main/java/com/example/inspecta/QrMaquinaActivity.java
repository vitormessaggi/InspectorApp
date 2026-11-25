package com.example.inspecta;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class QrMaquinaActivity extends AppCompatActivity {
    private static final String EXTRA_CODIGO = "codigoMaquina";

    public static class Exportacao extends ViewModel {
        private static final Executor ARQUIVOS = Executors.newSingleThreadExecutor();
        final MutableLiveData<Integer> estado = new MutableLiveData<>();

        void salvar(ContentResolver resolver, Uri destino, Bitmap qr) {
            estado.setValue(R.string.maquina_qr_salvando);
            ARQUIVOS.execute(() -> {
                try (OutputStream saida = resolver.openOutputStream(destino, "wt")) {
                    if (saida == null || !qr.compress(Bitmap.CompressFormat.PNG, 100, saida)) {
                        throw new IOException("PNG indisponível");
                    }
                } catch (IOException | SecurityException erro) {
                    estado.postValue(R.string.maquina_qr_erro_salvar);
                    return;
                }
                estado.postValue(R.string.maquina_qr_salvo);
            });
        }
    }

    static Intent paraMaquina(Context contexto, String codigo) {
        return new Intent(contexto, QrMaquinaActivity.class).putExtra(EXTRA_CODIGO, codigo);
    }

    private final ActivityResultLauncher<String> escolherArquivo =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"), this::receberDestino);
    private Exportacao exportacao;
    private Bitmap qr;
    private Button salvar;
    private TextView mensagem;
    private boolean escolhendoArquivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        if (codigo == null || codigo.trim().isEmpty()) {
            finish();
            return;
        }
        escolhendoArquivo = savedInstanceState != null && savedInstanceState.getBoolean("escolhendoArquivo");
        exportacao = new ViewModelProvider(this).get(Exportacao.class);
        Tela tela = new Tela(this, getString(R.string.maquina_qr_titulo), true);
        tela.texto(getString(R.string.maquina_qr_codigo, codigo));

        try {
            qr = criarImagem(QrMaquina.gerar(codigo));
            ImageView imagem = new ImageView(this);
            imagem.setImageBitmap(qr);
            imagem.setContentDescription(getString(R.string.maquina_qr_imagem, codigo));
            imagem.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int altura = Math.round(248 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams tamanho = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, altura);
            tamanho.gravity = Gravity.CENTER;
            tela.raiz.addView(imagem, tamanho);
        } catch (WriterException erro) {
            exportacao.estado.setValue(R.string.maquina_qr_erro_gerar);
        }

        mensagem = tela.texto("");
        mensagem.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        salvar = tela.botao(getString(R.string.maquina_qr_salvar), android.R.drawable.ic_menu_save,
                view -> selecionarDestino(codigo));
        tela.botao(getString(R.string.checklist_voltar), android.R.drawable.ic_menu_revert, view -> finish());
        exportacao.estado.observe(this, estado -> {
            mensagem.setText(estado);
            atualizarBotao();
        });
        atualizarBotao();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("escolhendoArquivo", escolhendoArquivo);
    }

    private void selecionarDestino(String codigo) {
        escolhendoArquivo = true;
        atualizarBotao();
        try {
            String nome = codigo.replaceAll("[^\\p{L}\\p{N}._-]", "_");
            escolherArquivo.launch("QR-" + nome + ".png");
        } catch (ActivityNotFoundException erro) {
            escolhendoArquivo = false;
            exportacao.estado.setValue(R.string.maquina_qr_erro_salvar);
        }
    }

    private void receberDestino(Uri destino) {
        escolhendoArquivo = false;
        if (destino != null && qr != null) {
            exportacao.salvar(getApplicationContext().getContentResolver(), destino, qr);
        }
        atualizarBotao();
    }

    private void atualizarBotao() {
        Integer estado = exportacao.estado.getValue();
        salvar.setEnabled(qr != null && !escolhendoArquivo
                && (estado == null || estado != R.string.maquina_qr_salvando));
    }

    private static Bitmap criarImagem(BitMatrix matriz) {
        int largura = matriz.getWidth();
        int altura = matriz.getHeight();
        int[] pixels = new int[largura * altura];
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                pixels[y * largura + x] = matriz.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        return Bitmap.createBitmap(pixels, largura, altura, Bitmap.Config.ARGB_8888);
    }
}
