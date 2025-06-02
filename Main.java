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
    private List<Integer> path;
    private AdjacencyList<Integer> G;

    public MaxCycle(AdjacencyList<Integer> G) {
        this.G = G;
        this.path = new ArrayList<>();
    }

    public List<Integer> bruteForceApproach(int root) {
        this.max = 0;
        this.path.clear();

        Set<Integer> P = new LinkedHashSet<>();
        P.add(root);
        bruteForce(P, root, root);

        return path;
    }

    private void bruteForce(Set<Integer> P, int root, int v) {
        for (int u : G.neighbors(v)) {
            if (u == root && P.size() >= 3 && P.size() > max) {
                max = P.size();
                path.clear();
                path.addAll(P);
                path.add(root);
                continue;
            }
            if (!P.contains(u)) {
                P.add(u);
                bruteForce(P, root, u);
                P.remove(u);
            }
        }
    }

     public List<Integer> branchAndBoundApproach(int root) {
        this.max = 0;
        this.path.clear();

        Set<Integer> P = new LinkedHashSet<>();
        P.add(root);
        branchAndBound(P, root, root);

        return path;
    }

    private void branchAndBound(Set<Integer> P, int root, int v) {
        for (int u : G.neighbors(v)) {
            if (u == root && P.size() >= 3 && P.size() > max) {
                max = P.size();
                path.clear();
                path.addAll(P);
                path.add(root);
                continue;
            }
            if (!P.contains(u)) {
                int upperbound = G.V().size();
                if(upperbound>max && G.degree(u)>1){
                    P.add(u);
                    branchAndBound(P, root, u);
                    P.remove(u);

                }
            }
        }
    }

    public List<Integer> parcialGreedyHeuristicApproach(int root) {
        this.max = 0;
        this.path.clear();

        if (isRootInvalid(root))
            return path;

        boolean usedBackup = false;
        int backupVertex = root;

        Set<Integer> P = new LinkedHashSet<>();

        int v = root;
        P.add(v);
        path.add(v);

        List<Integer> vOptions = new ArrayList<>();
        while (true) {
            vOptions.clear();

            for (int u : G.neighbors(v)) {
                if (u == root && path.size() >= 3) {
                    max = path.size();
                    path.add(u);
                    return path;
                } else if (!P.contains(u) && G.degree(u) > 1)
                    vOptions.add(u);
            }

            if (vOptions.isEmpty()) {
                if (usedBackup)
                    break;
                else {
                    usedBackup = true;
                    int backupIdx = path.indexOf(backupVertex);
                    if (backupIdx != -1) {
                        List<Integer> temp = new ArrayList<>(path.subList(0, backupIdx + 1));
                        path.clear();
                        path.addAll(temp);
                        v = backupVertex;
                        continue;
                    } else
                        break;
                }
            }
            if (!usedBackup && G.degree(v) > G.degree(backupVertex))
                backupVertex = v;

            int u = vOptions.stream().max(Comparator.comparingInt(G::degree)).orElseThrow();
            P.add(u);
            path.add(u);
            v = u;
        }

        return Collections.emptyList();
    }

    public List<Integer> randomizedHeuristicApproach(int root) {
        this.max = 0;
        this.path.clear();

        final int K = G.V().size() / 2;

        if (isRootInvalid(root))
            return this.path;

        Random randomGenerator = new Random();

        for (int i = 1; i <= K; i++) {
            Set<Integer> P = new LinkedHashSet<>();

            int v = root;
            P.add(v);

            boolean foundCycle = false;

            while (true) {
                List<Integer> vOptions = new ArrayList<>();
                for (int u : G.neighbors(v)) {
                    if (u == root && P.size() >= 3) {
                        if (P.size() > this.max) {
                            this.max = P.size();
                            this.path.clear();
                            this.path.addAll(P);
                            this.path.add(root);
                        }
                        foundCycle = true;
                        break;
                    } else if (!P.contains(u) && G.degree(u) > 1) {
                        vOptions.add(u);
                    }
                }
                if (foundCycle || vOptions.isEmpty())
                    break;

                int randomIndex = randomGenerator.nextInt(vOptions.size());
                int u = vOptions.get(randomIndex);

                P.add(u);
                v = u;
            }
        }
        return this.path;
    }

    private boolean isRootInvalid(int root) {
        if (!G.V().contains(root) || G.degree(root) <= 1)
            return true;
        long count = G.neighbors(root).stream().filter(neighbor -> G.degree(neighbor) > 2).count();
        return count < 2;
    }

    public int getMax() {
        return max;
    }

    public List<Integer> getMaxCyclePath() {
        return path;
    }
}

class MDS {
    private AdjacencyList<Integer> G;
    public Set<Integer> minDominatingSet;
    private int upperBound;

    MDS(AdjacencyList<Integer> G){
        this.G = G;
        minDominatingSet = new HashSet<Integer>();
    }

    int bruteForceApproach() {
        int n = G.V().size();
        Set<Integer> vertexSet = G.V();
        List<Integer> vertexList = new ArrayList<>(vertexSet); // Converte para Lista para acessar os índices
        long totalSubsets = 1 << n; // 2^n subsets

        List<Integer> MVC = new ArrayList<>();
        int minSize = Integer.MAX_VALUE;
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
            if (currSubset.size() < minSize && isVertexCover(currSubset)) { // Otimização para não verificar subsets
                                                                               // >= que o atual MVC
                minSize = currSubset.size();
                MVC = new ArrayList<>(currSubset);
                System.out.println("Nova menor Cobertura de Vértices encontrada: " + MVC + " (Tamanho: " + minSize + ")");
            }
        }

        return MVC.size();
    }

    public boolean isVertexCover(Set<Integer> subset) {
        List<List<Integer>> edges = G.getEdges();

        for (List<Integer> edge : edges) {
            int u = edge.get(0);
            int v = edge.get(1);

            if (!subset.contains(u) && !subset.contains(v)) {
                return false;
            }
        }
        return true;
    }

    public int branchAndBoundApproach(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices){
        // Condição de parada, encontrou conjunto dominante
        if (checkFullyDominated(currentInSet)) {
            if (currentInSet.size() < this.upperBound) {
                this.upperBound = currentInSet.size();
                this.minDominatingSet = new HashSet<>(currentInSet);
                System.out.println("MDS: " + this.minDominatingSet + ", Size: " + this.upperBound);
            }
            return currentInSet.size(); 
        }

        // Condição de Parada, sem vértices restantes e não é conjunto dominante
        if (undecidedVertices.isEmpty()) {
            return upperBound;
        }

        // Poda com base no limite inferior
        int lb = lowerBound(currentInSet, currentOutSet, undecidedVertices);
        if (lb >= this.upperBound) {
            return upperBound;
        }

        int nextVertex = -1;
        int maxDegreeFound = -1;

        // Cálculo do grau dinâmico
        // Próximo vértice é o que cobre mais vértices não dominados
        // Interseção entre vizinhos fechados e undecidedVertices
        Set<Integer> currentUndominated = new HashSet<>();
        for (int candidateVertex : undecidedVertices) { 
            for (int v : this.G.V()) {
                if (!isVertexDominated(v, currentInSet)) { 
                    currentUndominated.add(v); // Adiciona os vértices não dominados a um Set
                }
            }

            int currentDynamicDegree = 0;
            Set<Integer> closedNeighbors = this.G.closedNeighbors(candidateVertex);

            for (int v : closedNeighbors) { // Verifica se os vizinhos fechados do candidato estão sendo contabilizados
                if (currentUndominated.contains(v)) {
                    currentDynamicDegree++;
                }
            }

            // Atualiza o melhor candidato se o atual for melhor
            if (currentDynamicDegree > maxDegreeFound) {
                maxDegreeFound = currentDynamicDegree;
                nextVertex = candidateVertex;
            } 
        }
    
        Set<Integer> remainingUndecided = new HashSet<>(undecidedVertices);
        remainingUndecided.remove(nextVertex);

        // nextVertex no conjunto dominante
        Set<Integer> inSetBranch1 = new HashSet<>(currentInSet);
        inSetBranch1.add(nextVertex);
        
        int nextVertexIn = branchAndBoundApproach(inSetBranch1, currentOutSet, remainingUndecided);

        // nextVertex não está no conjunto dominante
        Set<Integer> outSetBranch2 = new HashSet<>(currentOutSet);
        outSetBranch2.add(nextVertex);
        int nextVertexOut = branchAndBoundApproach(currentInSet, outSetBranch2, remainingUndecided);

        return Math.min(nextVertexIn, nextVertexOut);
    }

    private boolean isVertexDominated(int vertex, Set<Integer> currentInSet) {
        if (currentInSet.contains(vertex)) {
            return true;
        }
        for (int v : this.G.neighbors(vertex)) {
            if (currentInSet.contains(v)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkFullyDominated(Set<Integer> currentInSet) {
        for (int v : this.G.V()) {
            if (!isVertexDominated(v, currentInSet)) {
                return false;
            }
        }
        return true;
    }

    private int lowerBound(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        int currentSetSize = currentInSet.size();

        // Encontrar vértices não dominados por currentInSet
        Set<Integer> notDominatedByCurrentSet = new HashSet<>();
        for (int v : this.G.V()) {
            if (!isVertexDominated(v, currentInSet)) {
                notDominatedByCurrentSet.add(v);
            }
        }
       
        // Verificar vértices que não são dominados e estão em currentOut
        for (int v : notDominatedByCurrentSet) {
            if (currentOutSet.contains(v)) {
                boolean canBeCoveredByUndecided = false;
                for (int u : this.G.neighbors(v)) {
                    if (undecidedVertices.contains(u)) { // Caso o vértice não possa ser dominado por seus vizinhos não existe solução válida
                        canBeCoveredByUndecided = true;
                        break;
                    }
                }
                if (!canBeCoveredByUndecided) {
                    return Integer.MAX_VALUE; 
                }
            }
        }
       
        int k = 0; // Vértice que cobre mais vértices não dominados
        for (int v : undecidedVertices) {

            Set<Integer> verticesDominatedByV = G.closedNeighbors(v);
            // Interseção de verticesDominatedByV com notDominatedByCurrentSet
            Set<Integer> intersection = new HashSet<>(verticesDominatedByV);
            intersection.retainAll(notDominatedByCurrentSet); // Operação de interseção

            int vCover = intersection.size();
            
            if (v > k) {
                k = vCover;
            }
        }
        if (k == 0) { 
            return Integer.MAX_VALUE;
        }

        // Calcular o número adicional de vértices necessários
        int minVerticesNeeded = (int) Math.ceil((double) notDominatedByCurrentSet.size() / k);
        
        return currentSetSize + minVerticesNeeded;
    }

    public int solveBranchAndBound(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices){
        this.upperBound = this.G.V().size() + 1;
        this.minDominatingSet = new HashSet<>();
        return branchAndBoundApproach(currentInSet, currentOutSet, undecidedVertices);
    }

}

class AdjacencyList<T> {
    private final Map<T, Set<T>> adj;
    private boolean useEdgeList;
    private List<List<T>> edges;
    private int numEdges;

    public int getNumEdges(){  return numEdges; }
    
    public AdjacencyList() {
        this.adj = new HashMap<>();
        this.useEdgeList = false;
    }

    public AdjacencyList(boolean useEdgeList) {
        this.useEdgeList = useEdgeList;
        this.adj = new HashMap<>();
        this.edges = new ArrayList<>();
        this.numEdges = 0;
    }

    public Set<T> neighbors(T v) {
        return adj.getOrDefault(v, Collections.emptySet());
    }

    public Set<T> closedNeighbors(T v){ // neighbors + v
        Set<T> closedNeighbors = new HashSet<>(neighbors(v));
        closedNeighbors.add(v);

        return closedNeighbors;
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
        if (useEdgeList){
            edges.add(List.of(v, u));
            numEdges++;
        }

        return a || b;
    }


    public int degree(T v) {
        return neighbors(v).size();
    }

    @Override
    public AdjacencyList<T> clone() {
        AdjacencyList<T> newGraph = new AdjacencyList<>();

        for (Map.Entry<T, Set<T>> entry : this.adj.entrySet()) {
            newGraph.adj.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        newGraph.numEdges = this.numEdges;

        return newGraph;
    }

    public List<List<T>> getEdges() {
        if (useEdgeList)
            return edges;
        throw new UnsupportedOperationException();
    }

    public T getMaxDegreeVertex(){
        Set<T> vertexSet = V();
        int maxDegree = -1;
        T maxDegreeVertex = null; 

        for(T v : vertexSet){
            int currentDegree = degree(v); 
            if(currentDegree > maxDegree){
                maxDegree = currentDegree;
                maxDegreeVertex = v;
            }
        }
        return maxDegreeVertex;
    }

    public boolean removeVertex(T v) {
        if (!adj.containsKey(v)) {
            System.out.println("Vértice " + v + " não encontrado.");
            return false; 
        }
        // Remover aresta 
        Set<T> neighborsOfV = adj.get(v);
        if (neighborsOfV != null) { 
            for (T neighbor : neighborsOfV) {
                if (adj.containsKey(neighbor)) {
                    adj.get(neighbor).remove(v); 
                    numEdges--;
                }
            }
        }

        // Remover 'v' do mapa de adjacência
        adj.remove(v);
        
        return true; 
    }

    public boolean removeMultipleVertex(Set<T> v) {
        for(T i : v){
            if(!removeVertex(i)){
                return false;
            }
        }
        return true;
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
        JMenuItem p1Heur1 = new JMenuItem("Heurístico Guloso");
        JMenuItem p1Heur2 = new JMenuItem("Heurístico Estocástico");

        p1Menu.add(p1BF);
        p1Menu.add(p1BnB);
        p1Menu.add(p1Heur1);
        p1Menu.add(p1Heur2);
        menuBar.add(p1Menu);

        JMenu p2Menu = new JMenu("Problema 2");
        JMenuItem p2Brute = new JMenuItem("Força Bruta");
        JMenuItem p2BnB = new JMenuItem("Branch and Bound");
        JMenuItem p2Heur = new JMenuItem("Heurístico");

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
        p1Heur1.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));
        p1Heur2.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));

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

                if (problem == 1) {
                    String name = stationInput.getText().trim();
                    Optional<Station> station = Station.findByName(stations, name);

                    if (station.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Estação \"" + name + "\" não encontrada.",
                                "Estação Inválida", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int id = station.get().id();
                    var G = generateSimpleAdjacencyList(false);

                    MaxCycle maxCycle = new MaxCycle(G);

                    SwingWorker<List<Integer>, Void> worker = new SwingWorker<>() {
                        @Override
                        protected List<Integer> doInBackground() {
                            return switch (approach) {
                                case "Força Bruta" -> maxCycle.bruteForceApproach(id);
                                case "Branch and Bound"->maxCycle.branchAndBoundApproach(id);
                                case "Heurístico Guloso" -> maxCycle.parcialGreedyHeuristicApproach(id);
                                case "Heurístico Estocástico" -> maxCycle.randomizedHeuristicApproach(id);
                                default -> throw new IllegalStateException("\"" + approach + "\"" + " não suportada.");
                            };
                        }

                        @Override
                        protected void done() {
                            try {
                                List<Integer> resultPathIds = get();

                                Map<Integer, String> stationNameMap = new HashMap<>();
                                stations.forEach(s -> stationNameMap.put(s.id(), s.name()));

                                StringBuilder sb = new StringBuilder("Caminho fechado: ");
                                resultPathIds.forEach(id -> sb.append(stationNameMap.get(id)).append(" -> "));
                                sb.setLength(sb.length() - 4); // Remove o ultimo " -> "
                                System.out.println(sb.toString());
                                int cycleSize = maxCycle.getMax();
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "O caminho fechado de maior cardinalidade possuí " + cycleSize + " vértices.");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "Erro na execução: " + ex.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    worker.execute();
                } else if (problem == 2) {
                    AdjacencyList<Integer> G = generateSimpleAdjacencyList(true);
                    MDS mds = new MDS(G);
                    SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Integer doInBackground() {
                            return switch (approach) {
                                case "Força Bruta" -> mds.bruteForceApproach();
                                case "Branch and Bound" -> mds.solveBranchAndBound(new HashSet<Integer>(), new HashSet<Integer>(), new HashSet<Integer>(G.V()));
                                default -> throw new IllegalStateException(
                                        "\"" + approach + "\"" + " não suportada para o Problema 2.");
                            };
                        }

                        @Override
                        protected void done() {
                            try {
                                int res = get();
            
                                List<Integer> resultPathIds = mds.minDominatingSet.stream().toList();
                                Map<Integer, String> stationNameMap = new HashMap<>();
                                stations.forEach(s -> stationNameMap.put(s.id(), s.name()));

                                StringBuilder sb = new StringBuilder("MDS: ");
                                resultPathIds.forEach(id -> sb.append(stationNameMap.get(id)).append(" -> "));
                                sb.setLength(sb.length() - 4); 
                                System.out.println(sb.toString());

                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "O Menor Conjunto Dominante possui " + res + " vértices.");
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

    AdjacencyList<Integer> generateSimpleAdjacencyList(boolean useEdgeList) {
        AdjacencyList<Integer> G = new AdjacencyList<>(useEdgeList);
        lines.forEach(edge -> {
            var s1 = Station.findByName(stations, edge.s1());
            var s2 = Station.findByName(stations, edge.s2());

            if (!s1.isPresent())
                System.out.println(edge.s1() + " não está presente");
            else if (!s2.isPresent())
                System.out.println(edge.s2() + " não está presente");

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
