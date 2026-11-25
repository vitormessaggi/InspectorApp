package com.example.inspecta;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutarChecklistActivity extends AppCompatActivity {
    static final String EXTRA_CHECKLIST_ID = "com.example.inspecta.extra.CHECKLIST_ID";

    private static final Executor ENVIOS_FOTO = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> permissaoCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), autorizada -> {
                if (autorizada) abrirCamera();
                else this.mensagemEnvio.setText(R.string.camera_permissao_necessaria);
            });

    private final ActivityResultLauncher<Uri> capturarFoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context contexto, @NonNull Uri destino) {
                    Intent camera = super.createIntent(contexto, destino);
                    camera.setClipData(ClipData.newRawUri("foto da inspeção", destino));
                    camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    return camera;
                }
            }, this::receberFoto);
    private final ChecklistStore checklists = new ChecklistStore();
    private final InspecaoStore inspecoes = new InspecaoStore();
    private LinearLayout perguntas;
    private TextView progresso;
    private TextView estado;
    private TextView mensagemEnvio;
    private EditText observacao;
    private Button enviar;
    private Button anexo;
    private TextView estadoFoto;
    private File pastaFotos;
    private File foto;
    private File capturaPendente;
    private GravacaoInspecao gravacao;
    private Checklist checklist;
    private String checklistId;
    static final String EXTRA_CODIGO = "codigoMaquina";
    private String inspecaoId;
    private int[] respostas;
    private Timestamp versaoRestaurada;
    private boolean statusConfirmado;
    private boolean bloqueada;
    private boolean erroStatus;
    private boolean concluida;
    private boolean enviouFalhaCritica;
    private AlertDialog confirmacaoEnvio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_executar_checklist);
        ChecklistUi.aplicarInsets(findViewById(R.id.raizChecklist));
        checklistId = getIntent().getStringExtra(EXTRA_CHECKLIST_ID);
        if (checklistId == null) {
            finish();
            return;
        }
        inspecaoId = savedInstanceState == null ? inspecoes.novoId() : savedInstanceState.getString("inspecaoId");
        pastaFotos = new File(getCacheDir(), "fotos-inspecao");
        if (savedInstanceState != null) {
            respostas = savedInstanceState.getIntArray("respostas");
            versaoRestaurada = savedInstanceState.getParcelable("versaoChecklist", Timestamp.class);
            concluida = savedInstanceState.getBoolean("concluida");
            enviouFalhaCritica = savedInstanceState.getBoolean("enviouFalhaCritica");
            String nomeFoto = savedInstanceState.getString("arquivoFoto");
            String nomePendente = savedInstanceState.getString("capturaPendente");
            if (nomeFoto != null) foto = new File(pastaFotos, nomeFoto);
            if (nomePendente != null) capturaPendente = new File(pastaFotos, nomePendente);
        }

        gravacao = new ViewModelProvider(this).get(GravacaoInspecao.class);
        perguntas = findViewById(R.id.listaPerguntas);
        progresso = findViewById(R.id.tvProgresso);
        estado = findViewById(R.id.tvInspecaoEstado);
        mensagemEnvio = findViewById(R.id.tvInspecaoEnvio);
        observacao = findViewById(R.id.etDescricaoFalha);
        enviar = findViewById(R.id.btnConcluir);
        anexo = findViewById(R.id.btnAnexarFoto);
        estadoFoto = findViewById(R.id.tvEstadoFoto);
        anexo.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamera();
            } else {
                permissaoCamera.launch(Manifest.permission.CAMERA);
            }
        });
        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        enviar.setOnClickListener(view -> enviarInspecao());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFinishing()) {
            return;
        }
        if (gravacao.tarefa != null) {
            acompanharEnvio();
        } else if (concluida) {
            mostrarRegistroEnviado();
            return;
        }
        if (checklist == null) {
            carregarChecklist();
        } else {
            observarBloqueio();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("inspecaoId", inspecaoId);
        outState.putIntArray("respostas", respostas);
        outState.putParcelable("versaoChecklist", checklist == null ? versaoRestaurada : checklist.atualizadoEm);
        outState.putBoolean("concluida", concluida);
        outState.putBoolean("enviouFalhaCritica", enviouFalhaCritica);
        if (foto != null) outState.putString("arquivoFoto", foto.getName());
        if (capturaPendente != null) outState.putString("capturaPendente", capturaPendente.getName());
    }

    private void carregarChecklist() {
        estado.setText(R.string.inspecao_carregando);
        checklists.buscar(checklistId).addOnCompleteListener(this, tarefa -> {
            if (concluida) {
                return;
            }
            checklist = tarefa.isSuccessful() ? Checklist.paraInspecao(tarefa.getResult()) : null;
            if (checklist == null) {
                estado.setText(R.string.inspecao_formulario_indisponivel);
                return;
            }
            if (respostas == null || respostas.length != checklist.itens.size()
                    || !Objects.equals(versaoRestaurada, checklist.atualizadoEm)) {
                respostas = new int[checklist.itens.size()];
            }
            String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
            CatalogoStore.resolver(checklist, codigo).addOnCompleteListener(this, maquina -> {
                if (!maquina.isSuccessful()) {
                    estado.setText("Máquina indisponível. Volte e atualize a lista.");
                    return;
                }
                checklist = maquina.getResult();
                montarPerguntas();
                observarBloqueio();
            });
        });
    }

    private void montarPerguntas() {
        ((TextView) findViewById(R.id.tvInspecaoNome)).setText(checklist.nome);
        ((TextView) findViewById(R.id.tvInspecaoMaquina)).setText(
                getString(R.string.inspecao_codigo_maquina, checklist.codigo, checklist.maquina));
        for (int i = 0; i < checklist.itens.size(); i++) {
            int indice = i;
            Checklist.Item item = checklist.itens.get(indice);
            View linha = getLayoutInflater().inflate(R.layout.item_pergunta, perguntas, false);
            linha.setSaveFromParentEnabled(false);
            ((TextView) linha.findViewById(R.id.tvTituloPergunta)).setText(item.pergunta);
            linha.findViewById(R.id.tvTagCritico).setVisibility(item.critico ? View.VISIBLE : View.GONE);
            RadioGroup grupo = linha.findViewById(R.id.rgResposta);
            if (respostas[indice] != 0) {
                grupo.check(respostas[indice] == Inspecao.FUNCIONANDO ? R.id.rbConforme : R.id.rbNaoConforme);
            }
            grupo.setOnCheckedChangeListener((view, selecionado) -> {
                respostas[indice] = selecionado == R.id.rbConforme ? Inspecao.FUNCIONANDO
                        : selecionado == R.id.rbNaoConforme ? Inspecao.NAO_FUNCIONANDO : 0;
                mensagemEnvio.setText("");
                atualizarFormulario();
            });
            perguntas.addView(linha);
        }
        observacao.setVisibility(View.VISIBLE);
        anexo.setVisibility(View.VISIBLE);
        estadoFoto.setVisibility(View.VISIBLE);
    }

    private void observarBloqueio() {
        statusConfirmado = false;
        erroStatus = false;
        atualizarFormulario();
        inspecoes.observarBloqueio(this, checklist.codigo, (existe, servidor) -> {
            bloqueada = existe;
            statusConfirmado = servidor;
            atualizarFormulario();
        }, () -> {
            statusConfirmado = false;
            erroStatus = true;
            atualizarFormulario();
        });
    }

    private void atualizarFormulario() {
        if (checklist == null || concluida) {
            return;
        }
        int respondidas = 0;
        boolean critica = false;
        for (int i = 0; i < respostas.length; i++) {
            if (respostas[i] != 0) {
                respondidas++;
            }
            critica |= respostas[i] == Inspecao.NAO_FUNCIONANDO && checklist.itens.get(i).critico;
        }
        progresso.setText(getString(R.string.inspecao_progresso, respondidas, respostas.length));
        estado.setText(bloqueada ? R.string.inspecao_maquina_bloqueada
                : erroStatus ? R.string.inspecao_erro_status
                : !statusConfirmado ? R.string.inspecao_verificando_status
                : critica ? R.string.inspecao_falha_bloqueia : R.string.inspecao_orientacao);
        enviar.setText(critica ? R.string.inspecao_enviar_falha : R.string.inspecao_enviar);

        boolean habilitado = statusConfirmado && !bloqueada && gravacao.tarefa == null;
        boolean configurada = CloudinaryFoto.configurada();
        boolean temFoto = foto != null && foto.length() > 0;
        boolean cameraFechada = capturaPendente == null;
        enviar.setEnabled(habilitado && configurada && cameraFechada && temFoto && respondidas == respostas.length);
        observacao.setEnabled(habilitado);
        anexo.setEnabled(habilitado && configurada && cameraFechada);
        anexo.setText(temFoto ? R.string.inspecao_foto_refazer : R.string.inspecao_foto_anexar);
        estadoFoto.setText(!configurada ? R.string.inspecao_foto_sem_configuracao
                : temFoto ? R.string.inspecao_foto_anexada : R.string.inspecao_foto_obrigatoria);
        for (int i = 0; i < perguntas.getChildCount(); i++) {
            View linha = perguntas.getChildAt(i);
            linha.findViewById(R.id.rbConforme).setEnabled(habilitado);
            linha.findViewById(R.id.rbNaoConforme).setEnabled(habilitado);
        }
    }

    private void abrirCamera() {
        if (isFinishing() || concluida || capturaPendente != null || gravacao.tarefa != null) return;
        try {
            if (!pastaFotos.isDirectory() && !pastaFotos.mkdirs()) {
                throw new IOException("Pasta de fotos indisponível");
            }
            capturaPendente = File.createTempFile("inspecao-", ".jpg", pastaFotos);
            Uri destino = FileProvider.getUriForFile(this, getPackageName() + ".fotos", capturaPendente);
            capturarFoto.launch(destino);
            atualizarFormulario();
        } catch (ActivityNotFoundException | SecurityException erro) {
            descartarCaptura();
            mensagemEnvio.setText(R.string.inspecao_foto_erro_camera);
        } catch (IOException erro) {
            descartarCaptura();
            mensagemEnvio.setText(R.string.inspecao_foto_erro_leitura);
        }
    }

    private void receberFoto(boolean capturou) {
        File arquivo = capturaPendente;
        capturaPendente = null;
        if (arquivo == null) return;

        if (capturou) {
            BitmapFactory.Options dimensoes = new BitmapFactory.Options();
            dimensoes.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(arquivo.getPath(), dimensoes);
            if (dimensoes.outWidth > 0 && dimensoes.outHeight > 0) {
                removerFoto();
                foto = arquivo;
                mensagemEnvio.setText("");
                atualizarFormulario();
                return;
            }
            mensagemEnvio.setText(R.string.inspecao_foto_erro_leitura);
        }
        arquivo.delete();
        atualizarFormulario();
    }

    private void descartarCaptura() {
        if (capturaPendente != null) capturaPendente.delete();
        capturaPendente = null;
    }

    private void removerFoto() {
        if (foto != null) foto.delete();
        foto = null;
        gravacao.fotoUrl = null;
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && gravacao != null) {
            descartarCaptura();
            if (gravacao.tarefa == null) {
                removerFoto();
            } else if (foto != null) {
                File arquivo = foto;
                gravacao.tarefa.addOnCompleteListener(tarefa -> arquivo.delete());
            }
        }
        super.onDestroy();
    }

    private void enviarInspecao() {
        if (foto == null || foto.length() == 0) {
            atualizarFormulario();
            mensagemEnvio.setText(R.string.inspecao_foto_obrigatoria);
            return;
        }
        if (!enviar.isEnabled()) return;

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            mensagemEnvio.setText(R.string.inspecao_sessao_expirada);
            return;
        }
        if (!ValidacaoInspecao.observacaoValida(respostas, observacao.getText().toString())) {
            observacao.setError("Descreva a não conformidade antes de enviar.");
            observacao.requestFocus();
            return;
        }
        enviouFalhaCritica = false;
        for (int i = 0; i < respostas.length; i++) {
            enviouFalhaCritica |= respostas[i] == Inspecao.NAO_FUNCIONANDO && checklist.itens.get(i).critico;
        }
        Checklist formulario = checklist;
        int[] respostasEnviadas = respostas.clone();
        String descricao = observacao.getText().toString().trim();
        String id = inspecaoId;
        String uid = usuario.getUid();
        InspecaoStore store = inspecoes;
        GravacaoInspecao envio = gravacao;
        File arquivo = foto;
        Task<String> upload = envio.fotoUrl == null
                ? Tasks.call(ENVIOS_FOTO, () -> CloudinaryFoto.enviar(arquivo))
                : Tasks.forResult(envio.fotoUrl);
        envio.tarefa = upload.onSuccessTask(url -> {
            envio.fotoUrl = url;
            return store.enviar(id, uid, formulario, respostasEnviadas, descricao, url);
        });
        acompanharEnvio();
    }

    private void acompanharEnvio() {
        mensagemEnvio.setText(R.string.inspecao_enviando);
        atualizarFormulario();
        gravacao.tarefa.addOnCompleteListener(this, tarefa -> {
            gravacao.tarefa = null;
            if (tarefa.isSuccessful()) {
                removerFoto();
                concluida = true;
                mostrarRegistroEnviado();
                return;
            }
            atualizarFormulario();
            int mensagem = R.string.inspecao_erro_envio;
            if (tarefa.getException() instanceof CloudinaryFoto.FotoInvalida) {
                mensagem = R.string.inspecao_foto_invalida;
            } else if (tarefa.getException() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code codigo = ((FirebaseFirestoreException) tarefa.getException()).getCode();
                if (codigo == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    mensagem = R.string.inspecao_maquina_bloqueada;
                } else if (codigo == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    mensagem = R.string.inspecao_formulario_alterado;
                }
            } else if (gravacao.fotoUrl == null) {
                mensagem = R.string.inspecao_foto_erro_envio;
            }
            mensagemEnvio.setText(mensagem);
        });
    }

    private void mostrarRegistroEnviado() {
        if (confirmacaoEnvio != null && confirmacaoEnvio.isShowing()) {
            return;
        }
        enviar.setEnabled(false);
        anexo.setEnabled(false);
        observacao.setEnabled(false);
        for (int i = 0; i < perguntas.getChildCount(); i++) {
            View linha = perguntas.getChildAt(i);
            linha.findViewById(R.id.rbConforme).setEnabled(false);
            linha.findViewById(R.id.rbNaoConforme).setEnabled(false);
        }
        confirmacaoEnvio = new AlertDialog.Builder(this)
                .setTitle(R.string.inspecao_enviada)
                .setMessage(enviouFalhaCritica ? R.string.inspecao_enviada_bloqueada : R.string.inspecao_enviada_registro)
                .setPositiveButton(android.R.string.ok, (dialogo, botao) -> finish())
                .setOnCancelListener(dialogo -> finish())
                .show();
    }
}
