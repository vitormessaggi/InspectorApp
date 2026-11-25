package com.example.inspecta;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
import java.util.Map;

final class QrMaquina {
    private static final int TAMANHO = 512;

    static BitMatrix gerar(String codigo) throws WriterException {
        return new QRCodeWriter().encode(codigo, BarcodeFormat.QR_CODE, TAMANHO, TAMANHO,
                Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                        EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
    }
}
