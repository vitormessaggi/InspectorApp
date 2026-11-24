package com.example.inspecta;

import org.junit.Test;
import static org.junit.Assert.*;

public class RegrasInspecaoTest {
    @Test public void naoConformidadeExigeObservacaoMesmoEntreItensConformes() {
        assertFalse(ValidacaoInspecao.observacaoValida(new int[]{1, 2, 1}, ""));
        assertFalse(ValidacaoInspecao.observacaoValida(new int[]{2}, " \n\t "));
        assertTrue(ValidacaoInspecao.observacaoValida(new int[]{1, 2}, "Vazamento na mangueira"));
    }
    @Test public void todosConformesPermitemObservacaoVazia() {
        assertTrue(ValidacaoInspecao.observacaoValida(new int[]{1, 1}, ""));
    }
    @Test public void bloqueioTemPrioridadeSobreInspecaoDoDia() {
        assertEquals("Bloqueada", SituacaoMaquina.status(true, true));
        assertEquals("Bloqueada", SituacaoMaquina.status(true, false));
        assertEquals("Sem inspeção", SituacaoMaquina.status(false, false));
        assertEquals("Liberada", SituacaoMaquina.status(false, true));
    }
    @Test public void indicadorUsaProporcaoDeItensERespeitaAmostraVazia() {
        assertEquals(75.0, SituacaoMaquina.percentual(3, 4), 0.001);
        assertEquals(0.0, SituacaoMaquina.percentual(0, 0), 0.001);
    }
}
