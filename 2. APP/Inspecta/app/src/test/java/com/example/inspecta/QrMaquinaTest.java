package com.example.inspecta;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QrMaquinaTest {
    @Test
    public void qrContemOCodigoCadastradoSemAlteracoes() throws Exception {
        String codigo = "TORNO/Ç-01%";
        BitMatrix qr = QrMaquina.gerar(codigo);
        int largura = qr.getWidth();
        int altura = qr.getHeight();
        int[] pixels = new int[largura * altura];
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                pixels[y * largura + x] = qr.get(x, y) ? 0xff000000 : 0xffffffff;
            }
        }
        BinaryBitmap imagem = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(largura, altura, pixels)));
        assertEquals(codigo, new QRCodeReader().decode(imagem).getText());
    }
}
