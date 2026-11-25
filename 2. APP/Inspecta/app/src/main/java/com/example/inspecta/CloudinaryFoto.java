package com.example.inspecta;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class CloudinaryFoto {
    private static final int LIMITE_BYTES = 10 * 1024 * 1024;
    private static final int DIMENSAO_REDUZIDA = 2048;
    private static final String ENTREGA = "https://res.cloudinary.com/";

    private CloudinaryFoto() {}

    static boolean configurada() {
        return !BuildConfig.CLOUDINARY_CLOUD_NAME.isEmpty() && !BuildConfig.CLOUDINARY_UPLOAD_PRESET.isEmpty();
    }

    static String enviar(File foto) throws Exception {
        byte[] arquivo = lerFoto(foto);
        if (arquivo.length == 0 || arquivo.length > LIMITE_BYTES) throw new FotoInvalida();

        String separador = UUID.randomUUID().toString();
        byte[] cabecalho = ("--" + separador + "\r\n"
                + "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n"
                + BuildConfig.CLOUDINARY_UPLOAD_PRESET + "\r\n"
                + "--" + separador + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"inspecao.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] rodape = ("\r\n--" + separador + "--\r\n").getBytes(StandardCharsets.UTF_8);
        URL destino = new URL("https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME + "/image/upload");
        HttpURLConnection conexao = (HttpURLConnection) destino.openConnection();
        try {
            conexao.setRequestMethod("POST");
            conexao.setInstanceFollowRedirects(false);
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(30_000);
            conexao.setDoOutput(true);
            conexao.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + separador);
            conexao.setFixedLengthStreamingMode(cabecalho.length + arquivo.length + rodape.length);
            try (OutputStream saida = conexao.getOutputStream()) {
                saida.write(cabecalho);
                saida.write(arquivo);
                saida.write(rodape);
            }
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Cloudinary retornou HTTP " + conexao.getResponseCode());
            }
            try (InputStream entrada = conexao.getInputStream()) {
                JSONObject resposta = new JSONObject(new String(entrada.readNBytes(65_536), StandardCharsets.UTF_8));
                String url = resposta.getString("secure_url");
                if (!urlValida(url) || !url.startsWith(ENTREGA + BuildConfig.CLOUDINARY_CLOUD_NAME + "/")) {
                    throw new IOException("URL de entrega inválida");
                }
                return url;
            }
        } finally {
            conexao.disconnect();
        }
    }

    private static byte[] lerFoto(File foto) throws IOException {
        if (foto.length() <= LIMITE_BYTES) {
            try (InputStream entrada = new FileInputStream(foto)) {
                byte[] dados = entrada.readNBytes(LIMITE_BYTES + 1);
                boolean jpeg = dados.length >= 3 && dados[0] == (byte) 0xff
                        && dados[1] == (byte) 0xd8 && dados[2] == (byte) 0xff;
                if (jpeg && dados.length <= LIMITE_BYTES) return dados;
            }
        }

        Bitmap imagem = ImageDecoder.decodeBitmap(ImageDecoder.createSource(foto), (decoder, info, source) -> {
            int largura = info.getSize().getWidth();
            int altura = info.getSize().getHeight();
            float escala = Math.min(1f, (float) DIMENSAO_REDUZIDA / Math.max(largura, altura));
            decoder.setTargetSize(Math.max(1, Math.round(largura * escala)),
                    Math.max(1, Math.round(altura * escala)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            if (!imagem.compress(Bitmap.CompressFormat.JPEG, 88, saida)) throw new FotoInvalida();
            return saida.toByteArray();
        } finally {
            imagem.recycle();
        }
    }

    static boolean urlValida(String url) {
        return url != null && url.length() <= 2048
                && url.matches("https://res[.]cloudinary[.]com/[A-Za-z0-9_-]+/image/upload/[A-Za-z0-9_./%~-]+");
    }

    static void abrir(Context contexto, String url) {
        if (!urlValida(url)) return;
        try {
            contexto.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException erro) {
            Toast.makeText(contexto, R.string.inspecao_foto_erro_abrir, Toast.LENGTH_SHORT).show();
        }
    }

    static final class FotoInvalida extends IOException {}
}
