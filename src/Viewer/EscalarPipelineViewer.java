package src.Viewer;
import java.awt.*;
import javax.swing.*;

import src.ArquiteturaModel.Instruction;

import java.util.ArrayList;

public class EscalarPipelineViewer extends JFrame {

    private JPanel pipelinePanel;
    private ArrayList<JLabel> stageLabels;
    private JButton runButton;
    private JButton pauseButton; // Botão Pause
    private JButton stopButton; // Botão Stop
    private JComboBox<String> architectureComboBox; // ComboBox para seleção da arquitetura (IMT, BMT, REF)
    private JComboBox<String> modeComboBox; // ComboBox para seleção entre ESCALAR e SUPERESCALAR

    // Novas labels para as métricas
    private JLabel cpiLabel;
    private JLabel totalCyclesLabel;
    private JLabel bubbleCyclesLabel;

    // Variáveis para armazenar os valores das métricas
    public EscalarPipelineViewer() {
        setTitle("Visualização do Pipeline");
        setSize(1200, 500); // Ajustado para acomodar melhor os componentes
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Adiciona espaçamento entre os componentes

        // Painel para a linha de estágios
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridLayout(1, 5)); // 5 estágios: IF, ID, EX, MEM, WB
        String[] stages = { "IF", "ID", "EX", "MEM", "WB" };
        for (String stage : stages) {
            JLabel headerLabel = new JLabel(stage, SwingConstants.CENTER);
            headerLabel.setOpaque(true);
            headerLabel.setBackground(Color.GRAY);
            headerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            headerLabel.setFont(new Font("Arial", Font.BOLD, 14)); // Aumenta a fonte para melhor visibilidade
            headerPanel.add(headerLabel);
        }

        // Painel principal para os estágios do pipeline
        pipelinePanel = new JPanel();
        pipelinePanel.setLayout(new GridLayout(1, 5, 10, 10)); // 5 estágios: IF, ID, EX, MEM, WB
        stageLabels = new ArrayList<>();

        // Adicionando labels para os estágios
        for (@SuppressWarnings("unused") String stage : stages) {
            JLabel label = new JLabel("NOP", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(Color.LIGHT_GRAY);
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            label.setFont(new Font("Arial", Font.PLAIN, 12)); // Ajusta a fonte
            pipelinePanel.add(label);
            stageLabels.add(label);
        }

        // Painel para as métricas
        JPanel metricsPanel = new JPanel();
        metricsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5)); // Layout horizontal com espaçamento
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Métricas de Desempenho"));

        cpiLabel = new JLabel("CPI: 0.0");
        bubbleCyclesLabel = new JLabel("Ciclos de Bolha: 0");
        totalCyclesLabel = new JLabel("Total de Ciclos: 0");

        // Ajusta a fonte das métricas
        cpiLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        bubbleCyclesLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        totalCyclesLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        metricsPanel.add(cpiLabel);
        metricsPanel.add(bubbleCyclesLabel);
        metricsPanel.add(totalCyclesLabel);

        // Painel combinado para pipeline e métricas
        JPanel pipelineAndMetricsPanel = new JPanel();
        pipelineAndMetricsPanel.setLayout(new BorderLayout());
        pipelineAndMetricsPanel.add(pipelinePanel, BorderLayout.CENTER);
        pipelineAndMetricsPanel.add(metricsPanel, BorderLayout.SOUTH); // Métricas abaixo do pipeline

        // Painel para os controles
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        runButton = new JButton("Run");
        controlPanel.add(runButton);

        pauseButton = new JButton("Pause");
        pauseButton.setEnabled(false); // Inicialmente desabilitado
        controlPanel.add(pauseButton);

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false); // Inicialmente desabilitado
        controlPanel.add(stopButton);

        // Adiciona ComboBox para selecionar a arquitetura específica
        architectureComboBox = new JComboBox<>(new String[] { "IMT", "BMT", "REF","SMT" });
        controlPanel.add(new JLabel("Arquitetura:"));
        controlPanel.add(architectureComboBox);

        // Adiciona ComboBox para selecionar modo (ESCALAR ou SUPERESCALAR)
        modeComboBox = new JComboBox<>(new String[] { "ESCALAR", "SUPERESCALAR" });
        controlPanel.add(new JLabel("Modo:"));
        controlPanel.add(modeComboBox);

        // Layout principal
        add(headerPanel, BorderLayout.NORTH);
        add(pipelineAndMetricsPanel, BorderLayout.CENTER); // Pipeline e métricas no centro
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * Atualiza o pipeline ESCALAR no visualizador.
     * 
     * @param pipeline Lista de instruções no pipeline
     * @param cycle    Ciclo atual da simulação
     */
    public void updatePipeline(ArrayList<Instruction> pipeline, int cycle) {
        for (int i = 0; i < stageLabels.size(); i++) {
            int currentIndex = cycle - i;
            if (currentIndex >= 0 && currentIndex < pipeline.size()) {
                Instruction instr = pipeline.get(currentIndex);
                stageLabels.get(i).setText("<html>" + instr.codigo + " " + instr.dest + ", " + instr.op1 + ", " + instr.op2
                + "<br>(Thread " + instr.contexto + ")</html>");
                stageLabels.get(i).setBackground(generateColor(instr.contexto + 1));
                if (instr.inst.equals("BUB")) {
                    stageLabels.get(i).setText(" BUB");
                    stageLabels.get(i).setBackground(Color.RED);
                }
            } else {
                stageLabels.get(i).setText("NOP");
                stageLabels.get(i).setBackground(Color.LIGHT_GRAY);
            }
        }
        setTitle("Pipeline - Ciclo " + (cycle + 1));
    }

    /**
     * Gera uma cor baseada no índice do contexto.
     * 
     * @param index Índice do contexto
     * @return Objeto Color correspondente
     */
    private Color generateColor(int index) {
        int r = (index * 50) % 256; // Vermelho
        int g = (index * 100) % 256; // Verde
        int b = (index * 150) % 256; // Azul
        return new Color(r, g, b);
    }

    // Métodos para atualizar as métricas
    public void updateCPI(double cpi) {
        cpiLabel.setText(String.format("CPI: %.2f", cpi));
    }

    public void updateTotalCycles(int totalCycles) {
        totalCyclesLabel.setText("Total de Ciclos: " + totalCycles);
    }

    public void updateBubbleCycles(int bubbleCycles) {
        bubbleCyclesLabel.setText("Ciclos de Bolha: " + bubbleCycles);
    }

    // Getters para os botões
    public JButton getRunButton() {
        return runButton;
    }

    public JButton getPauseButton() {
        return pauseButton;
    }

    public JButton getStopButton() {
        return stopButton;
    }

    public String getSelectedArchitecture() {
        return (String) architectureComboBox.getSelectedItem();
    }

    public String getSelectedMode() {
        return (String) modeComboBox.getSelectedItem();
    }
}