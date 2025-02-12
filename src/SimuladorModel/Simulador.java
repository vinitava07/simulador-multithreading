package src.SimuladorModel;
import java.awt.event.*;
import javax.swing.*;

import src.ArquiteturaModel.Instruction;
import src.EscalarModel.Escalar;
import src.SuperescalarModel.SuperEscalar;
import src.Viewer.EscalarPipelineViewer;
import src.Viewer.SimplePipelineViewer;

import java.util.ArrayList;
public class Simulador {

    public static volatile boolean isPaused = false; // Flag de pausa
    public static final Object pauseLock = new Object(); // Lock para pausar
    public static SwingWorker<Void, Void> currentWorker; // Tornado público para acesso externo

    public static void main(String[] args) {
        // Inicializando a interface gráfica
        EscalarPipelineViewer viewer = new EscalarPipelineViewer();

        // Adicionando ActionListener ao botão Run
        viewer.getRunButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedArch = viewer.getSelectedArchitecture();
                String selectedMode = viewer.getSelectedMode();

                if (selectedMode.equals("SUPERESCALAR")) {
                    // Modo SUPERESCALAR: Abrir a visualização superescalar sem iniciar a simulação
                    SuperEscalar selectedPipeline;
                    if (selectedArch.equals("BMT")) {
                        System.out.println("Preparando BMTPipeline para Superescalar...");
                        selectedPipeline = new SuperEscalar(3, 3, false);
                    } else if (selectedArch.equals("IMT")) {
                        System.out.println("Preparando IMTPipeline para Superescalar...");
                        selectedPipeline = new SuperEscalar(1, 3, false);
                    } else if (selectedArch.equals("REF")) { // REF
                        System.out.println("Preparando REF Pipeline para Superescalar...");
                        selectedPipeline = new SuperEscalar(Integer.MAX_VALUE, 3, false);
                    } else if (selectedArch.equals("SMT")) {
                        System.out.println("Preparando SMTPipeline para Superescalar");
                        selectedPipeline = new SuperEscalar(0, 3, true);
                    } else {
                        System.out.println("ERRO, não suportado");
                        return;
                    }
                    // Passar a referência da interface principal para a interface superescalar
                    SimplePipelineViewer spv = new SimplePipelineViewer(selectedPipeline, viewer);
                    spv.setVisible(true);

                    // Desabilitar o botão Run principal para evitar múltiplas simulações
                    viewer.getRunButton().setEnabled(false);

                    return; // Encerra a execução para evitar iniciar a simulação aqui
                }

                // Caso contrário, modo ESCALAR: Iniciar a simulação normalmente
                // Desabilita o botão Run e habilita Pause e Stop
                viewer.getRunButton().setEnabled(false);
                viewer.getPauseButton().setEnabled(true);
                viewer.getStopButton().setEnabled(true);

                ArrayList<Instruction> selectedPipeline;

                Escalar escalar = new Escalar();
                if (selectedArch.equals("BMT")) {
                    escalar.createBMTPipeline(); // Cria BMTPipeline
                    escalar.printPipeline(1);
                    selectedPipeline = escalar.bmtPipeline;
                    System.out.println("Executando BMTPipeline...");
                } else if (selectedArch.equals("IMT")) {
                    escalar.createIMTPipeline(); // Cria IMTPipeline
                    escalar.printPipeline(0);
                    selectedPipeline = escalar.imtPipeline;
                    System.out.println("Executando IMTPipeline...");
                } else if (selectedArch.equals("REF")) { // REF
                    escalar.createREFPipeline(); // Cria REF Pipeline
                    escalar.printPipeline(2);
                    selectedPipeline = escalar.refPipeline;
                    System.out.println("Executando REF Pipeline...");
                } else {
                    JOptionPane.showMessageDialog(viewer, "Não é suportado esse modo", "ERRO",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int totalCiclos = selectedPipeline.size() + 5; // 5 estágios

                // Inicializar contadores de métricas
                final int[] totalCycles = { 0 };
                final int[] bubbleCycles = { 0 };
                final int[] executedInstructions = { 0 };

                // Criar e iniciar o SwingWorker para a simulação escalar
                currentWorker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        for (int i = 0; i < totalCiclos; i++) {
                            if (isCancelled()) {
                                break;
                            }

                            // Pausa a simulação se necessário
                            synchronized (pauseLock) {
                                while (isPaused) {
                                    try {
                                        pauseLock.wait();
                                    } catch (InterruptedException ex) {
                                        if (isCancelled()) {
                                            break;
                                        }
                                    }
                                }
                            }

                            final int cycle = i;
                            SwingUtilities.invokeLater(() -> viewer.updatePipeline(selectedPipeline, cycle));

                            // Atualizar os contadores
                            totalCycles[0]++;
                            // Verificar se há bolhas (BUB) no ciclo atual
                            if (selectedPipeline.size() > cycle) {
                                Instruction instr = selectedPipeline.get(cycle);
                                if (instr.inst.equals("BUB")) {
                                    bubbleCycles[0]++;
                                } else {
                                    executedInstructions[0]++;
                                }
                            }

                            // Calcular CPI
                            double cpi = executedInstructions[0] > 0 ? (double) totalCycles[0] / executedInstructions[0]
                                    : 0.0;

                            // Atualizar as métricas na interface gráfica
                            SwingUtilities.invokeLater(() -> {
                                viewer.updateTotalCycles(totalCycles[0]);
                                viewer.updateBubbleCycles(bubbleCycles[0]);
                                viewer.updateCPI(cpi);
                            });

                            // Simula o avanço dos ciclos
                            try {
                                Thread.sleep(500); // 0.5 segundos por ciclo
                            } catch (InterruptedException ex) {
                                if (isCancelled()) {
                                    break;
                                }
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        // Redefinir a flag de pausa
                        isPaused = false;
                        synchronized (pauseLock) {
                            pauseLock.notifyAll();
                        }

                        viewer.getRunButton().setEnabled(true);
                        viewer.getPauseButton().setEnabled(false);
                        viewer.getStopButton().setEnabled(false);
                        viewer.getPauseButton().setText("Pause");
                        try {
                            get(); // Verifica se houve exceções
                            if (!isCancelled()) {
                                JOptionPane.showMessageDialog(viewer, "Simulação concluída!", "Fim",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (Exception ex) {
                            if (isCancelled()) {
                                JOptionPane.showMessageDialog(viewer, "Simulação interrompida!", "Interrompida",
                                        JOptionPane.WARNING_MESSAGE);
                            } else {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(viewer, "Erro na simulação!", "Erro",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                };
                currentWorker.execute();
            }
        });

        // Adicionando ActionListener ao botão Pause
        viewer.getPauseButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentWorker == null) {
                    return;
                }

                if (!isPaused) {
                    // Pausar a simulação
                    isPaused = true;
                    viewer.getPauseButton().setText("Resume");
                } else {
                    // Retomar a simulação
                    synchronized (pauseLock) {
                        isPaused = false;
                        pauseLock.notifyAll();
                    }
                    viewer.getPauseButton().setText("Pause");
                }
            }
        });

        // Adicionando ActionListener ao botão Stop
        viewer.getStopButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentWorker != null && !currentWorker.isDone()) {
                    currentWorker.cancel(true);
                }

                // Redefinir a flag de pausa e notificar todas as threads aguardando
                isPaused = false;
                synchronized (pauseLock) {
                    pauseLock.notifyAll();
                }

                // Resetar os botões
                viewer.getRunButton().setEnabled(true);
                viewer.getPauseButton().setEnabled(false);
                viewer.getPauseButton().setText("Pause");
                viewer.getStopButton().setEnabled(false);
            }
        });
    }
}