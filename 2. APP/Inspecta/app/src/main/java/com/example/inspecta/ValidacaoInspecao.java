package com.example.inspecta;

final class ValidacaoInspecao {
    static boolean observacaoValida(int[] respostas, String observacao) {
        for (int resposta : respostas) {
            if (resposta == 2 && (observacao == null || observacao.trim().isEmpty())) return false;
        }
        return true;
    }
}
