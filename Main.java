import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.BorderLayout;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}

class MaxCycle {
    private int max;
    private Set<Integer> maxCycle;
    private AdjacencyList<Integer> G;

    public MaxCycle(AdjacencyList<Integer> G) {
        this.G = G;
    }

    public int bruteForceApproach(int root) {
        max = 0;
        maxCycle = new HashSet<>();

        Set<Integer> P = new HashSet<>();
        P.add(root);
        bruteForce(P, root, root);
        P.forEach(System.out::println);
        return max;
    }

    private void bruteForce(Set<Integer> P, int root, int v) {
        for (int u : G.neighbors(v)) {
            if (u == root && P.size() >= 3 && P.size() > max) {
                max = P.size();
                maxCycle.clear();
                maxCycle.addAll(P);
                continue;
            }
            if (!P.contains(u)) {
                P.add(u);
                bruteForce(P, root, u);
                P.remove(u);
            }
        }
    }

    public int getMax() {
        return max;
    }

    public Set<Integer> getMaxCyclePath() {
        return maxCycle;
    }
}

class MVC {
    static int n;
    
    static List<Integer> bruteForceApproach(AdjacencyListMVC<Integer> G) {
        n = G.V().size();
        Set<Integer> vertexSet = G.V();
        List<Integer> vertexList = new ArrayList<>(vertexSet); // Converte para Lista para acessar os índices
        long totalSubsets = 1 << n; // 2^n subsets

        List<Integer> MVC = new ArrayList<>();
        int minSize = Integer.MAX_VALUE;
        System.out.println("Tamanho do Grafo: " + n);
        // Itera de 0 a 2^n - 1
        for (long i = 0; i < totalSubsets; i++) {
            Set<Integer> currSubset = new HashSet<>();
            // Para cada número 'i', verifica cada bit para construir o subset
            for (int j = 0; j < n; j++) {
                // Se o j-ésimo bit de 'i' for 1, inclui o j-ésimo elemento do conjunto
                if ((i & (1 << j)) > 0) {
                    currSubset.add(vertexList.get(j));
                }
            }
            // Verificar se o subset atual é vertex cover
            if(currSubset.size() < minSize && isVertexCover(currSubset, G)){ // Otimização para não verificar subsets >= que o atual MVC
                minSize = currSubset.size();
                MVC = new ArrayList<>(currSubset);
                System.out.println("Nova menor Cobertura de Vértices encontrada: " + MVC + " (Tamanho: " + minSize + ")");
            }
        }

        return MVC;
    }

    public static boolean isVertexCover(Set<Integer> subset, AdjacencyListMVC<Integer> G) {
        List<List<Integer>> edges = G.getEdges();

        for(List<Integer> edge : edges){
           int u = edge.get(0);
           int v = edge.get(1);

           if(!subset.contains(u) && !subset.contains(v)){
                return false;
           }
        }
        return true;
    }
}

class AdjacencyList<T> {
    private final Map<T, Set<T>> adj = new HashMap<>();

    public Set<T> neighbors(T v) {
        return adj.getOrDefault(v, Collections.emptySet());
    }

    public Set<T> V() {
        return adj.keySet();
    }

    public boolean addEdge(T v, T u) {
        if (!adj.containsKey(v))
            adj.put(v, new HashSet<>());
        if (!adj.containsKey(u))
            adj.put(u, new HashSet<>());
        var a = adj.get(v).add(u);
        var b = adj.get(u).add(v);
        return a || b;
    }

    public int degree(T v) {
        return neighbors(v).size();
    }
}

class AdjacencyListMVC<T> {
    private final Map<T, Set<T>> adj = new HashMap<>();
    private List<List<T>> edges = new ArrayList<>();

    public List<List<T>> getEdges(){  return edges; }

    public Set<T> neighbors(T v) {
        return adj.getOrDefault(v, Collections.emptySet());
    }

    public Set<T> V() {
        return adj.keySet();
    }

    public boolean addEdge(T v, T u) {
        if (!adj.containsKey(v))
            adj.put(v, new HashSet<>());
        if (!adj.containsKey(u))
            adj.put(u, new HashSet<>());
        var a = adj.get(v).add(u);
        var b = adj.get(u).add(v);
        edges.add(List.of(v, u));
        return a || b;
    }

    public int degree(T v) {
        return neighbors(v).size();
    }

    public void print() {
        for (Map.Entry<T, Set<T>> entry : adj.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (T neighbor : entry.getValue()) {
                System.out.print(neighbor + " ");
            }
            System.out.println();
        }
    }
}

record Cordinates(int x, int y) {
}

record Station(int id, String name, Cordinates position) {
    static Optional<Station> findByName(List<Station> stations, String name) {
        return stations.stream().filter(s -> s.name.equals(name)).findFirst();
    }
}

record Line(String s1, String s2, String name, Color color) {
}

class GUI extends JFrame {
    static final int FRAME_WIDTH = 800;
    static final int FRAME_HEIGHT = 600;

    static final Pattern STATION_LINE_REGEX = Pattern.compile("\"([^\"]+)\"\\s+(\\d+)\\s+(\\d+)");
    static final Pattern LINE_LINE_HEADER_REGEX = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\d+)");
    static final Pattern LINE_LINE_BODY_REGEX = Pattern.compile("\"([^\"]+)\"\\s+\"([^\"]+)\"");

    static final Map<String, Color> colorMap = Map.ofEntries(
            Map.entry("azul", Color.BLUE),
            Map.entry("vermelha", Color.RED),
            Map.entry("verde", Color.GREEN),
            Map.entry("amarela", Color.YELLOW),
            Map.entry("laranja", Color.ORANGE),
            Map.entry("marrom", new Color(139, 69, 19)),
            Map.entry("roxo", new Color(128, 0, 128)),
            Map.entry("lilás", new Color(200, 162, 200)),
            Map.entry("rosa", new Color(255, 105, 180)),
            Map.entry("rosa-choque", new Color(255, 20, 147)),
            Map.entry("verde-musgo", new Color(85, 107, 47)));

    JPanel mainPanel;

    List<Station> stations = new ArrayList<>();
    List<Line> lines = new ArrayList<>();

    GUI() {
        setTitle("Passe de Metro de Paris - Solver");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();

        JMenu fMenu = new JMenu("Ler Arquivos");
        JMenuItem fopenStations = new JMenuItem("Ler Estações do Metro");
        JMenuItem fopenLines = new JMenuItem("Ler Linhas do Metro");
        JMenuItem exit = new JMenuItem("Sair");

        fMenu.add(fopenStations);
        fMenu.add(fopenLines);
        fMenu.addSeparator();
        fMenu.add(exit);
        menuBar.add(fMenu);

        JMenu p1Menu = new JMenu("Problema 1");
        JMenuItem p1BF = new JMenuItem("Força Bruta");
        JMenuItem p1BnB = new JMenuItem("Branch and Bound");
        JMenuItem p1Heur = new JMenuItem("Aproximado");

        p1Menu.add(p1BF);
        p1Menu.add(p1BnB);
        p1Menu.add(p1Heur);
        menuBar.add(p1Menu);

        JMenu p2Menu = new JMenu("Problema 2");
        JMenuItem p2Brute = new JMenuItem("Força Bruta");
        JMenuItem p2BnB = new JMenuItem("Branch and Bound");
        JMenuItem p2Heur = new JMenuItem("Aproximado");

        p2Menu.add(p2Brute);
        p2Menu.add(p2BnB);
        p2Menu.add(p2Heur);
        menuBar.add(p2Menu);

        setJMenuBar(menuBar);

        fopenStations.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setFileFilter(new FileFilterImpl("_stations.txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                stations = readStationFile(chooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, stations.size() + " estações carregadas");
            }
        });

        fopenLines.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setFileFilter(new FileFilterImpl("_lines.txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                lines = readLineFile(chooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, lines.size() + " linhas carregadas");

                GraphPanel graphPanel = new GraphPanel(stations, lines);
                JScrollPane graphPanelScroll = new JScrollPane(graphPanel);
                graphPanelScroll.getVerticalScrollBar().setUnitIncrement(16);

                mainPanel.removeAll();
                mainPanel.add(graphPanelScroll, BorderLayout.CENTER);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });

        exit.addActionListener(e -> System.exit(0));

        p1BF.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));
        p1BnB.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));
        p1Heur.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));

        p2Brute.addActionListener(e -> new ProblemSolverDialog(this, 2, ((JMenuItem) e.getSource()).getText()));
        p2BnB.addActionListener(e -> new ProblemSolverDialog(this, 2, ((JMenuItem) e.getSource()).getText()));
        p2Heur.addActionListener(e -> new ProblemSolverDialog(this, 2, ((JMenuItem) e.getSource()).getText()));

        setVisible(true);
    }

    class ProblemSolverDialog extends JDialog {
        static final int DIALOG_INIT_WIDTH = 480;
        static final int DIALOG_INIT_HEIGHT = 360;

        ProblemSolverDialog(JFrame owner, int problem, String approach) {
            super(owner, "Problema " + Integer.toString(problem), false);
            setLayout(new BorderLayout());
            setSize(DIALOG_INIT_WIDTH, DIALOG_INIT_HEIGHT);
            setLocationRelativeTo(owner);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

            JLabel title = new JLabel("Técnica de Projeto escolhida: " + approach);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(title);

            content.add(Box.createVerticalStrut(10));

            String context;
            String goal;
            switch (problem) {
                case 1 -> {
                    context = "Determinar o número máximo de estações que um turista poderá visitar com um só passe, saindo e retornando de uma determinada estação.";
                    goal = "Encontrar o conjunto de vértices de maior cardinalidade que formam um caminho fechado no grafo partindo de um determinado vértice raiz.";
                }
                case 2 -> {
                    context = "Determinar as estações para instalação de guichês para venda de passes, de modo que um turista não precise caminhar mais que uma estação para encontrar um guichê. Devem ser determinados o número mínimo de guichês a serem instalados e as estações que devem recebe-los.";
                    goal = "Encontrar o minimum vertex cover set do grafo e sua respectiva cardinalidade.";
                }
                default -> {
                    context = "Error";
                    goal = "Error";
                }
            }

            JLabel contextLabel = new JLabel("Contextualização:");
            contextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(contextLabel);
            JTextArea contextArea = new JTextArea(context);
            contextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            contextArea.setLineWrap(true);
            contextArea.setWrapStyleWord(true);
            contextArea.setEditable(false);
            contextArea.setOpaque(false);
            content.add(contextArea);
            content.add(Box.createVerticalStrut(10));
            JLabel goalLabel = new JLabel("Objetivo:");
            goalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(goalLabel);

            JTextArea goalArea = new JTextArea(goal);
            goalArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            goalArea.setLineWrap(true);
            goalArea.setWrapStyleWord(true);
            goalArea.setEditable(false);
            goalArea.setOpaque(false);
            content.add(goalArea);

            content.add(Box.createVerticalStrut(10));
            JLabel stationLabel = new JLabel("Digite o nome da estação raiz:");
            content.add(Box.createVerticalStrut(10));

            JTextField stationInput = new JTextField();

            stationInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            content.add(Box.createVerticalStrut(10));

            if (problem == 1) {
                content.add(stationLabel);
                content.add(stationInput);
            }

            JButton runButton = new JButton("Executar");
            content.add(runButton);

            add(content, BorderLayout.CENTER);

            runButton.addActionListener(e -> {
                if (stations.isEmpty() || lines.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "É preciso carregar as entradas de estações e de linhas do metro.",
                            "Nenhum input foi fornecido ainda", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (problem == 1) { // Lógica para o Problema 1
                    String name = stationInput.getText().trim();
                    Optional<Station> station = Station.findByName(stations, name);

                    if (station.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Estação \"" + name + "\" não encontrada.",
                                "Estação Inválida", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int id = station.get().id();
                    AdjacencyList<Integer> graph = generateSimpleAdjacencyList();

                SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Integer doInBackground() {
                        return switch (approach) {
                            case "Força Bruta" -> null;
                            default -> throw new IllegalStateException("\"" + approach + "\"" + " não suportada.");
                        };
                    }

                        @Override
                        protected void done() {
                            try {
                                int res = get();
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "O caminho fechado de maior cardinalidade possuí " + res + " vértices.");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "Erro na execução: " + ex.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    worker.execute();
                } else if (problem == 2) { // Lógica para o Problema 2
                    AdjacencyListMVC<Integer> graphMVC = generateSimpleAdjacencyListMVC();
                    graphMVC.print();
                    SwingWorker<List<Integer>, Void> worker = new SwingWorker<>() { 
                        @Override
                        protected List<Integer> doInBackground() {
                            return switch (approach) {
                                case "Força Bruta" -> MVC.bruteForceApproach(graphMVC);
                                default -> throw new IllegalStateException("\"" + approach + "\"" + " não suportada para o Problema 2.");
                            };
                        }

                        @Override
                         protected void done() {
                            try {
                                List<Integer> res = get();
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "O MVC possui " + res.size() + " vértices.");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "Erro na execução: " + ex.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    worker.execute();
                }
            });
            setVisible(true);
        }
    }

    AdjacencyList<Integer> generateSimpleAdjacencyList() {
        AdjacencyList<Integer> G = new AdjacencyList<>();
        lines.forEach(edge -> {
            var s1 = Station.findByName(stations, edge.s1());
            var s2 = Station.findByName(stations, edge.s2());
            if(!s1.isPresent()) {
                System.out.println(edge.s1() + " não está presente");
            }else if(!s2.isPresent()) {
                System.out.println(edge.s2() + " não está presente");
            }
        
            int v = Station.findByName(stations, edge.s1()).get().id();
            int u = Station.findByName(stations, edge.s2()).get().id();
            G.addEdge(v, u);
        });
        return G;
    }

    AdjacencyListMVC<Integer> generateSimpleAdjacencyListMVC() {
        AdjacencyListMVC<Integer> G = new AdjacencyListMVC<>();
        lines.forEach(edge -> {
            var s1 = Station.findByName(stations, edge.s1());
            var s2 = Station.findByName(stations, edge.s2());
            if(!s1.isPresent()) {
                System.out.println(edge.s1() + " não está presente");
            }else if(!s2.isPresent()) {
                System.out.println(edge.s2() + " não está presente");
            }
        
            int v = Station.findByName(stations, edge.s1()).get().id();
            int u = Station.findByName(stations, edge.s2()).get().id();
            G.addEdge(v, u);
        });
        return G;
    }

    List<Station> readStationFile(String filename) {
        int id = 0;
        List<Station> stations = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = STATION_LINE_REGEX.matcher(line.trim());
                if (!m.matches())
                    throw new IllegalStateException("Linha de estacoes de metro inválida -> " + line);
                stations.add(new Station(id++, m.group(1),
                        new Cordinates(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)))));
            }
        } catch (Exception e) {
            System.err.println("Erro as estacoes do metro =(\n" + e.getMessage());
        }
        return stations;
    }

    List<Line> readLineFile(String filename) {
        List<Line> result = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher h = LINE_LINE_HEADER_REGEX.matcher(line.trim());
                if (!h.matches())
                    throw new IllegalStateException("Linha header de linha de metro inválida -> " + line);
                String lineName = h.group(1);
                Color lineColor = colorMap.get(h.group(2).toLowerCase());
                int segCount = Integer.parseInt(h.group(3));

                for (int i = 0; i < segCount && (line = br.readLine()) != null; i++) {
                    Matcher b = LINE_LINE_BODY_REGEX.matcher(line.trim());
                    if (!b.matches())
                        throw new IllegalStateException("Linha body de linha de metro inválida -> " + line);
                    result.add(new Line(b.group(1), b.group(2), lineName, lineColor));
                }
            }
        } catch (Exception e) {
            System.err.println("Erro as linhas do metro =(\n" + e.getMessage());
        }
        return result;
    }

    class FileFilterImpl extends FileFilter {
        private final String suffix;

        public FileFilterImpl(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(suffix);
        }

        @Override
        public String getDescription() {
            return "*" + suffix;
        }
    }

    class GraphPanel extends JPanel {
        static final Dimension GRAPH_SIZE = new Dimension(1600, 1600);

        static final int NODE_DIAMETER = 12;
        static final Function<Integer, Integer> CENTER_NODE_POS = pos -> pos - NODE_DIAMETER / 2;
        static final Color NODE_COLOR = Color.BLACK;

        static final int LABEL_PADDING = 4;
        static final BasicStroke EDGE_STROKE = new BasicStroke(4);

        List<Station> stations;
        List<Line> lines;

        GraphPanel(List<Station> stations, List<Line> lines) {
            this.stations = stations;
            this.lines = lines;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            lines.forEach(l -> paintEdge(g2d, l));
            stations.forEach(s -> paintNode(g2d, s));
        }

        void paintEdge(Graphics2D g, Line uv) {
            var v = Station.findByName(stations, uv.s1());
            var u = Station.findByName(stations, uv.s2());
            if (v.isPresent() && u.isPresent()) {
                Cordinates vPos = v.get().position();
                Cordinates uPos = u.get().position();
                g.setColor(uv.color());
                g.setStroke(EDGE_STROKE);
                g.drawLine(vPos.x(), vPos.y(), uPos.x(), uPos.y());
            }
        }

        void paintNode(Graphics2D g, Station v) {
            g.setColor(Color.black);
            int cx = CENTER_NODE_POS.apply(v.position().x());
            int cy = CENTER_NODE_POS.apply(v.position().y());
            g.fillOval(cx, cy, NODE_DIAMETER, NODE_DIAMETER);
            paintNodeLabel(g, cx, cy, v.name());
        }

        void paintNodeLabel(Graphics2D g, int cx, int cy, String label) {
            int textWidth = label.length();
            int textX = cx - textWidth;
            int textY = cy + 2 * NODE_DIAMETER;
            g.drawString(label, textX, textY);
        }

        @Override
        public Dimension getPreferredSize() {
            return GRAPH_SIZE;
        }
    }
}
