package com.example.inspecta;

final class SituacaoMaquina {
    static String status(boolean bloqueada, boolean inspecionadaHoje) {
        if (bloqueada) return "Bloqueada";
        return inspecionadaHoje ? "Liberada" : "Sem inspeção";
    }

    static double percentual(long conformes, long total) {
        return total == 0 ? 0 : 100.0 * conformes / total;
    }
}
