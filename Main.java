/*
 * Metro de Paris -
 * 
 * Alunos:
 * 
 * Eduardo Araújo Valadares Silva
 * Henrique Resende Lara
 * Luigi Louback de Oliveira
 * Thomas Neuenschwander Maciel Baron
 * 
 */
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
    private int max; // Guarda o tamanho do maior ciclo encontrado
    private List<Integer> C; // Armazena o caminho do maior ciclo encontrado
    private final AdjacencyList<Integer> graph; // Grafo para busca

    // Construtor recebe o grafo onde será feita a busca
    public MaxCycle(AdjacencyList<Integer> graph) {
        this.graph = graph;
        this.C = new ArrayList<>();
    }

    // Inicia busca exaustiva a partir do vértice s
    public List<Integer> bruteForceApproach(int s) {
        this.max = 0;
        this.C.clear();

        Set<Integer> P = new LinkedHashSet<>(); // Caminho atual, mantendo ordem de visita
        P.add(s); // Começa pelo vértice raiz
        bruteForce(P, s, s);

        return C; // Retorna o maior ciclo encontrado
    }

    // Função recursiva que explora todos os caminhos possíveis para achar ciclos maiores
    private void bruteForce(Set<Integer> P, int s, int v) {
        for (int u : graph.neighbors(v)) { // Para cada vizinho de v
            if (u == s && P.size() >= 3 && P.size() > max) {
                // Ciclo fechado maior que o atual max encontrado
                max = P.size();
                C.clear();
                C.addAll(P);
                C.add(s); // Fecha o ciclo adicionando o vértice raiz
                continue;
            }
            if (!P.contains(u)) {
                // Continua expandindo o caminho se o vizinho ainda não foi visitado
                P.add(u);
                bruteForce(P, s, u);
                P.remove(u); // Backtracking: remove o vizinho após explorar
            }
        }
    }

    // Busca otimizada com poda (branch and bound)
    public List<Integer> branchAndBoundApproach(int s) {
        this.max = 0;
        this.C.clear();

        Set<Integer> P = new LinkedHashSet<>();
        P.add(s);
        branchAndBound(P, s, s);

        return C;
    }

    // Recursão com poda: só expande se ainda existir chance de achar ciclo maior
    private void branchAndBound(Set<Integer> P, int s, int v) {
        for (int u : graph.neighbors(v)) {
            if (u == s && P.size() >= 3 && P.size() > max) {
                max = P.size();
                C.clear();
                C.addAll(P);
                C.add(s);
                continue;
            }
            if (!P.contains(u)) {
                int upperBound = graph.V().size(); // Tamanho máximo possível do ciclo
                // Poda: só expande se max ainda pode ser superado e grau do vizinho > 1
                if (upperBound > max && graph.degree(u) > 1) {
                    P.add(u);
                    branchAndBound(P, s, u);
                    P.remove(u);
                }
            }
        }
    }

    // Heurística gulosa parcial: tenta construir um ciclo grande, priorizando vizinhos de maior grau
    public List<Integer> parcialGreedyHeuristicApproach(int s) {
        this.max = 0;
        this.C.clear();

        if (isRootInvalid(s))
            return C; // Raiz inválida: não tenta busca

        boolean usedBackup = false; // Marca se já tentou rota alternativa
        int backupVertex = s; // Guarda vértice para backup

        Set<Integer> P = new LinkedHashSet<>();
        int v = s;
        P.add(v);
        C.add(v);

        List<Integer> vOptions = new ArrayList<>();
        while (true) {
            vOptions.clear();

            // Reúne vizinhos válidos para tentar expandir caminho
            for (int u : graph.neighbors(v)) {
                if (u == s && C.size() >= 3) {
                    max = C.size();
                    C.add(u);
                    return C; // Ciclo fechado, retorna solução
                } else if (!P.contains(u) && graph.degree(u) > 1)
                    vOptions.add(u);
            }

            if (vOptions.isEmpty()) { // Sem opções para expandir
                if (usedBackup)
                    break; // Já tentou backup, finaliza
                usedBackup = true;
                int backupIdx = C.indexOf(backupVertex);
                if (backupIdx != -1) {
                    // Retorna para backup para tentar outro caminho
                    List<Integer> temp = new ArrayList<>(C.subList(0, backupIdx + 1));
                    C.clear();
                    C.addAll(temp);
                    v = backupVertex;
                    continue;
                } else
                    break;
            }
            // Atualiza backup para tentar melhor rota
            if (!usedBackup && graph.degree(v) > graph.degree(backupVertex))
                backupVertex = v;

            // Escolhe próximo vértice com maior grau
            int u = vOptions.stream().max(Comparator.comparingInt(graph::degree)).orElseThrow();
            P.add(u);
            C.add(u);
            v = u;
        }
        return Collections.emptyList();
    }

    // Heurística estocástica: realiza várias tentativas aleatórias para achar ciclos grandes
    public List<Integer> randomizedHeuristicApproach(int s) {
        this.max = 0; 
        this.C.clear(); 

        final int maxIterations = graph.V().size() / 2; // Número de tentativas aleatórias

        if (isRootInvalid(s)) // Verifica se o vértice raiz é válido para iniciar busca
            return C;

        Random random = new Random();

        for (int i = 0; i < maxIterations; i++) {
            Set<Integer> P = new LinkedHashSet<>(); // Caminho atual, sem repetição, com ordem
            int v = s;
            P.add(v);
            boolean foundCycle = false;

            while (true) {
                List<Integer> vOptions = new ArrayList<>();
                for (int u : graph.neighbors(v)) {
                    // Se possível fechar ciclo com raiz e ciclo tiver tamanho mínimo
                    if (u == s && P.size() >= 3) {
                        // Atualiza o maior ciclo encontrado
                        if (P.size() > max) {
                            max = P.size();
                            C.clear();
                            C.addAll(P);
                            C.add(s); // Fecha o ciclo
                        }
                        foundCycle = true;
                        break;
                    } else if (!P.contains(u) && graph.degree(u) > 1) {
                        // Coleta vizinhos ainda não visitados com grau > 1
                        vOptions.add(u);
                    }
                }
                // Sai se ciclo foi fechado ou não há para onde ir
                if (foundCycle || vOptions.isEmpty())
                    break;

                // Escolhe vizinho aleatório para continuar caminho
                int randomIndex = random.nextInt(vOptions.size());
                int u = vOptions.get(randomIndex);
                P.add(u);
                v = u;
            }
        }
        return C; // Retorna o maior ciclo encontrado nas tentativas
    }

    // Verifica se vértice raiz tem grau suficiente para iniciar busca
    private boolean isRootInvalid(int s) {
        if (!graph.V().contains(s) || graph.degree(s) <= 1)
            return true;
        long count = graph.neighbors(s).stream()
                .filter(neighbor -> graph.degree(neighbor) > 2)
                .count();
        return count < 2;
    }

    public int getMax() {
        return max;
    }

    public List<Integer> getMaxCyclePath() {
        return C;
    }
}

class MDS {
    private final AdjacencyList<Integer> graph;
    public Set<Integer> minDominatingSet;
    private int upperBound;

    public MDS(AdjacencyList<Integer> graph) {
        this.graph = graph;
        this.minDominatingSet = new HashSet<>();
    }

    public List<Integer> bruteForceApproach(AdjacencyList<Integer> graph) {
        Set<Integer> allVertices = graph.V();
        int numVertices = allVertices.size();
        List<Integer> vertexList = new ArrayList<>(allVertices);

        List<Integer> bestSolution = null;
        int minSizeFound = numVertices + 1;

        System.out.println("Grafo possui " + numVertices + " vértices");

        long maxSubsets = 1L << numVertices;
        for (long mask = 0; mask < maxSubsets; mask++) {
            List<Integer> currentSet = new ArrayList<>();
            for (int i = 0; i < numVertices; i++) {
                if ((mask & (1L << i)) != 0) {
                    currentSet.add(vertexList.get(i));
                }
            }

            if (currentSet.size() < minSizeFound && isValidDominatingSet(currentSet, graph)) {
                minSizeFound = currentSet.size();
                bestSolution = new ArrayList<>(currentSet);
                System.out.println("Novo menor conjunto dominante: " + bestSolution + " (tamanho: " + minSizeFound + ")");
            }
        }

        if (bestSolution == null) {
            System.out.println("Nenhum conjunto dominante encontrado, retornando lista vazia");
            return new ArrayList<>();
        }

        System.out.println("Conjunto dominante mínimo final: " + bestSolution + " (tamanho: " + minSizeFound + ")");
        return bestSolution;
    }

    public boolean isValidDominatingSet(List<Integer> subset, AdjacencyList<Integer> graph) {
        Set<Integer> coveredVertices = new HashSet<>(subset);

        for (Integer vertex : graph.V()) {
            if (!coveredVertices.contains(vertex)) {
                boolean hasNeighborInSet = false;
                for (Integer neighbor : graph.neighbors(vertex)) {
                    if (subset.contains(neighbor)) {
                        hasNeighborInSet = true;
                        break;
                    }
                }
                if (!hasNeighborInSet) return false;

                coveredVertices.add(vertex);
            }
        }
        return true;
    }

    public int branchAndBoundApproach(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        if (checkFullyDominated(currentInSet)) {
            if (currentInSet.size() < this.upperBound) {
                this.upperBound = currentInSet.size();
                this.minDominatingSet = new HashSet<>(currentInSet);
                System.out.println("MDS: " + this.minDominatingSet + ", Size: " + this.upperBound);
            }
            return currentInSet.size();
        }

        if (undecidedVertices.isEmpty()) {
            return upperBound;
        }

        int lowerBound = lowerBound(currentInSet, currentOutSet, undecidedVertices);
        if (lowerBound >= this.upperBound) {
            return upperBound;
        }

        int nextVertex = -1;
        int maxCoverage = -1;

        Set<Integer> currentlyUndominated = new HashSet<>();
        for (int v : this.graph.V()) {
            if (!isVertexDominated(v, currentInSet)) {
                currentlyUndominated.add(v);
            }
        }

        for (int candidate : undecidedVertices) {
            Set<Integer> closedNeighbors = this.graph.closedNeighbors(candidate);
            int coverageCount = 0;
            for (int neighbor : closedNeighbors) {
                if (currentlyUndominated.contains(neighbor)) {
                    coverageCount++;
                }
            }
            if (coverageCount > maxCoverage) {
                maxCoverage = coverageCount;
                nextVertex = candidate;
            }
        }

        Set<Integer> remainingUndecided = new HashSet<>(undecidedVertices);
        remainingUndecided.remove(nextVertex);

        Set<Integer> inSetBranch = new HashSet<>(currentInSet);
        inSetBranch.add(nextVertex);
        int branchInResult = branchAndBoundApproach(inSetBranch, currentOutSet, remainingUndecided);

        Set<Integer> outSetBranch = new HashSet<>(currentOutSet);
        outSetBranch.add(nextVertex);
        int branchOutResult = branchAndBoundApproach(currentInSet, outSetBranch, remainingUndecided);

        return Math.min(branchInResult, branchOutResult);
    }

    private boolean isVertexDominated(int vertex, Set<Integer> currentInSet) {
        if (currentInSet.contains(vertex)) return true;
        for (int neighbor : this.graph.neighbors(vertex)) {
            if (currentInSet.contains(neighbor)) return true;
        }
        return false;
    }

    private boolean checkFullyDominated(Set<Integer> currentInSet) {
        for (int vertex : this.graph.V()) {
            if (!isVertexDominated(vertex, currentInSet)) return false;
        }
        return true;
    }

    private int lowerBound(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        int currentSize = currentInSet.size();

        Set<Integer> notDominated = new HashSet<>();
        for (int v : this.graph.V()) {
            if (!isVertexDominated(v, currentInSet)) {
                notDominated.add(v);
            }
        }

        for (int v : notDominated) {
            if (currentOutSet.contains(v)) {
                boolean canBeCovered = false;
                for (int neighbor : this.graph.neighbors(v)) {
                    if (undecidedVertices.contains(neighbor)) {
                        canBeCovered = true;
                        break;
                    }
                }
                if (!canBeCovered) return Integer.MAX_VALUE;
            }
        }

        int maxCoverage = 0;
        for (int v : undecidedVertices) {
            Set<Integer> closedNeighbors = graph.closedNeighbors(v);
            Set<Integer> intersection = new HashSet<>(closedNeighbors);
            intersection.retainAll(notDominated);
            int coverage = intersection.size();
            if (coverage > maxCoverage) {
                maxCoverage = coverage;
            }
        }

        if (maxCoverage == 0) return Integer.MAX_VALUE;

        int minVerticesNeeded = (int) Math.ceil((double) notDominated.size() / maxCoverage);
        return currentSize + minVerticesNeeded;
    }

    public List<Integer> solveBranchAndBound(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        this.upperBound = this.graph.V().size() + 1;
        this.minDominatingSet = new HashSet<>();
        branchAndBoundApproach(currentInSet, currentOutSet, undecidedVertices);
        return new ArrayList<>(this.minDominatingSet);
    }

    public List<Integer> iteratedGreedyApproach(AdjacencyList<Integer> graph, int maxIterations, int removalSize) {
        Random random = new Random();

        Set<Integer> solution = new HashSet<>();
        List<List<Integer>> edges = graph.getEdges();
        Set<List<Integer>> uncoveredEdges = new HashSet<>(edges);

        while (!uncoveredEdges.isEmpty()) {
            Map<Integer, Integer> coverageCount = new HashMap<>();
            for (List<Integer> edge : uncoveredEdges) {
                int u = edge.get(0), v = edge.get(1);
                coverageCount.put(u, coverageCount.getOrDefault(u, 0) + 1);
                coverageCount.put(v, coverageCount.getOrDefault(v, 0) + 1);
            }

            int bestVertex = coverageCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get()
                    .getKey();

            solution.add(bestVertex);
            uncoveredEdges.removeIf(edge -> edge.contains(bestVertex));
        }

        List<Integer> bestSolution = new ArrayList<>(solution);

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<Integer> partialSolution = new ArrayList<>(solution);
            for (int i = 0; i < removalSize && !partialSolution.isEmpty(); i++) {
                int indexToRemove = random.nextInt(partialSolution.size());
                partialSolution.remove(indexToRemove);
            }

            Set<Integer> rebuiltSolution = new HashSet<>(partialSolution);
            Set<List<Integer>> uncovered = new HashSet<>();
            for (List<Integer> edge : edges) {
                if (!rebuiltSolution.contains(edge.get(0)) && !rebuiltSolution.contains(edge.get(1))) {
                    uncovered.add(edge);
                }
            }

            while (!uncovered.isEmpty()) {
                Map<Integer, Integer> coverageCount = new HashMap<>();
                for (List<Integer> edge : uncovered) {
                    int u = edge.get(0), v = edge.get(1);
                    coverageCount.put(u, coverageCount.getOrDefault(u, 0) + 1);
                    coverageCount.put(v, coverageCount.getOrDefault(v, 0) + 1);
                }

                int bestVertex = coverageCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();

                rebuiltSolution.add(bestVertex);
                uncovered.removeIf(edge -> edge.contains(bestVertex));
            }

            solution = rebuiltSolution;
            if (solution.size() < bestSolution.size()) {
                bestSolution = new ArrayList<>(solution);
            }
        }

        System.out.println("Melhor solução encontrada: " + bestSolution + " (Tamanho: " + bestSolution.size() + ")");
        return bestSolution;
    }
}

class AdjacencyList<T> {
    private final Map<T, Set<T>> adjacency;
    private final boolean useEdgeList;
    private List<List<T>> edges;
    private int numEdges;

    public int getNumEdges() {
        return numEdges;
    }

    public AdjacencyList() {
        this.adjacency = new HashMap<>();
        this.useEdgeList = false;
    }

    public AdjacencyList(boolean useEdgeList) {
        this.useEdgeList = useEdgeList;
        this.adjacency = new HashMap<>();
        this.edges = new ArrayList<>();
        this.numEdges = 0;
    }

    public Set<T> neighbors(T vertex) {
        return adjacency.getOrDefault(vertex, Collections.emptySet());
    }

    public Set<T> closedNeighbors(T vertex) {
        Set<T> closed = new HashSet<>(neighbors(vertex));
        closed.add(vertex);
        return closed;
    }

    public Set<T> V() {
        return adjacency.keySet();
    }

    public boolean addEdge(T v, T u) {
        adjacency.putIfAbsent(v, new HashSet<>());
        adjacency.putIfAbsent(u, new HashSet<>());

        boolean addedV = adjacency.get(v).add(u);
        boolean addedU = adjacency.get(u).add(v);

        if (useEdgeList) {
            edges.add(List.of(v, u));
            numEdges++;
        }

        return addedV || addedU;
    }

    public int degree(T vertex) {
        return neighbors(vertex).size();
    }

    @Override
    public AdjacencyList<T> clone() {
        AdjacencyList<T> newGraph = new AdjacencyList<>();
        for (Map.Entry<T, Set<T>> entry : this.adjacency.entrySet()) {
            newGraph.adjacency.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        newGraph.numEdges = this.numEdges;
        return newGraph;
    }

    public List<List<T>> getEdges() {
        if (useEdgeList) return edges;
        throw new UnsupportedOperationException("Edge list is not enabled.");
    }

    public boolean removeVertex(T vertex) {
        if (!adjacency.containsKey(vertex)) {
            System.out.println("Vértice " + vertex + " não encontrado.");
            return false;
        }

        Set<T> neighborsOfVertex = adjacency.get(vertex);
        if (neighborsOfVertex != null) {
            for (T neighbor : neighborsOfVertex) {
                if (adjacency.containsKey(neighbor)) {
                    adjacency.get(neighbor).remove(vertex);
                    numEdges--;
                }
            }
        }

        adjacency.remove(vertex);
        return true;
    }

    public boolean removeMultipleVertex(Set<T> vertices) {
        for (T vertex : vertices) {
            if (!removeVertex(vertex)) {
                return false;
            }
        }
        return true;
    }

    public void print() {
        for (Map.Entry<T, Set<T>> entry : adjacency.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (T neighbor : entry.getValue()) {
                System.out.print(neighbor + " ");
            }
            System.out.println();
        }
    }
}

record Cordinates(int x, int y) {}

record Station(int id, String name, Cordinates position) {
    static Optional<Station> findByName(List<Station> stations, String name) {
        return stations.stream().filter(s -> s.name.equals(name)).findFirst();
    }
}

record Line(String s1, String s2, String name, Color color) {}

class GUI extends JFrame {
    static final int FRAME_WIDTH = 800;
    static final int FRAME_HEIGHT = 600;

    static final Pattern STATION_LINE_REGEX = Pattern.compile("\"([^\"]+)\"\\s+(\\d+)\\s+(\\d+)");
    static final Pattern LINE_LINE_HEADER_REGEX = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\d+)");
    static final Pattern LINE_LINE_BODY_REGEX = Pattern.compile("\"([^\"]+)\"\\s+\"([^\"]+)\"");

    Set<Integer> highlightedVertices = new HashSet<>();
    Set<List<Integer>> highlightedEdges = new HashSet<>();
    AdjacencyList<Integer> graphForHighlighting;

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
            Map.entry("verde-musgo", new Color(85, 107, 47))
    );

    JPanel mainPanel;
    List<Station> stations = new ArrayList<>();
    List<Line> lines = new ArrayList<>();

    public GUI() {
        setTitle("Passe de Metro de Paris - Solver");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Ler Arquivos");
        JMenuItem openStations = new JMenuItem("Ler Estações do Metro");
        JMenuItem openLines = new JMenuItem("Ler Linhas do Metro");
        JMenuItem exitItem = new JMenuItem("Sair");

        fileMenu.add(openStations);
        fileMenu.add(openLines);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu problem1Menu = new JMenu("Problema 1");
        JMenuItem p1BruteForce = new JMenuItem("Força Bruta");
        JMenuItem p1BranchAndBound = new JMenuItem("Branch and Bound");
        JMenuItem p1GreedyHeuristic = new JMenuItem("Heurístico Guloso");
        JMenuItem p1RandomHeuristic = new JMenuItem("Heurístico Estocástico");

        problem1Menu.add(p1BruteForce);
        problem1Menu.add(p1BranchAndBound);
        problem1Menu.add(p1GreedyHeuristic);
        problem1Menu.add(p1RandomHeuristic);
        menuBar.add(problem1Menu);

        JMenu problem2Menu = new JMenu("Problema 2");
        JMenuItem p2BruteForce = new JMenuItem("Força Bruta");
        JMenuItem p2BranchAndBound = new JMenuItem("Branch and Bound");
        JMenuItem p2IteratedGreedy = new JMenuItem("Heurístico (Iterated Greedy Algorithm)");

        problem2Menu.add(p2BruteForce);
        problem2Menu.add(p2BranchAndBound);
        problem2Menu.add(p2IteratedGreedy);
        menuBar.add(problem2Menu);

        setJMenuBar(menuBar);

        openStations.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setFileFilter(new FileFilterImpl("_stations.txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                stations = readStationFile(chooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, stations.size() + " estações carregadas");
            }
        });

        openLines.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setFileFilter(new FileFilterImpl("_lines.txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                lines = readLineFile(chooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, lines.size() + " linhas carregadas");

                GraphPanel graphPanel = new GraphPanel(stations, lines);
                JScrollPane scrollPane = new JScrollPane(graphPanel);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);

                mainPanel.removeAll();
                mainPanel.add(scrollPane, BorderLayout.CENTER);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });

        exitItem.addActionListener(e -> System.exit(0));

        p1BruteForce.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));
        p1BranchAndBound.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));
        p1GreedyHeuristic.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));
        p1RandomHeuristic.addActionListener(e -> new ProblemSolverDialog(this, 1, ((JMenuItem) e.getSource()).getText()));

        p2BruteForce.addActionListener(e -> new ProblemSolverDialog(this, 2, ((JMenuItem) e.getSource()).getText()));
        p2BranchAndBound.addActionListener(e -> new ProblemSolverDialog(this, 2, ((JMenuItem) e.getSource()).getText()));
        p2IteratedGreedy.addActionListener(e -> new ProblemSolverDialog(this, 2, ((JMenuItem) e.getSource()).getText()));

        setVisible(true);
    }

    private Map<Integer, String> buildStationNameMap() {
        Map<Integer, String> stationNameMap = new HashMap<>();
        for (Station s : stations) {
            stationNameMap.put(s.id(), s.name());
        }
        return stationNameMap;
    }

    class ProblemSolverDialog extends JDialog {
        static final int DIALOG_INIT_WIDTH = 480;
        static final int DIALOG_INIT_HEIGHT = 360;

        ProblemSolverDialog(JFrame owner, int problem, String approach) {
            super(owner, "Problema " + problem, false);
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
                    goal = "Encontrar o menor conjunto dominante do grafo e sua respectiva cardinalidade.";
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

                    int rootId = station.get().id();
                    AdjacencyList<Integer> graph = generateSimpleAdjacencyList(false);
                    MaxCycle maxCycle = new MaxCycle(graph);

                    SwingWorker<List<Integer>, Void> worker = new SwingWorker<>() {
                        @Override
                        protected List<Integer> doInBackground() {
                            return switch (approach) {
                                case "Força Bruta" -> maxCycle.bruteForceApproach(rootId);
                                case "Branch and Bound" -> maxCycle.branchAndBoundApproach(rootId);
                                case "Heurístico Guloso" -> maxCycle.parcialGreedyHeuristicApproach(rootId);
                                case "Heurístico Estocástico" -> maxCycle.randomizedHeuristicApproach(rootId);
                                default -> throw new IllegalStateException("\"" + approach + "\"" + " não suportada.");
                            };
                        }

                        @Override
                        protected void done() {
                            try {
                                List<Integer> resultPath = get();

                                Map<Integer, String> stationNameMap = buildStationNameMap();

                                StringBuilder sb = new StringBuilder("Caminho fechado: ");
                                resultPath.forEach(id -> sb.append(stationNameMap.get(id)).append(" -> "));
                                sb.setLength(sb.length() - 4);
                                System.out.println(sb);

                                int cycleSize = maxCycle.getMax();

                                highlightedVertices.clear();
                                highlightedEdges.clear();

                                if (!resultPath.isEmpty()) {
                                    highlightedVertices.add(resultPath.get(0));

                                    for (int i = 0; i < resultPath.size() - 1; i++) {
                                        int v1 = resultPath.get(i);
                                        int v2 = resultPath.get(i + 1);
                                        List<Integer> edge = v1 < v2 ? List.of(v1, v2) : List.of(v2, v1);
                                        highlightedEdges.add(edge);
                                    }
                                }

                                mainPanel.repaint();

                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "O caminho fechado de maior cardinalidade possui " + cycleSize + " vértices.");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "Erro na execução: " + ex.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    worker.execute();

                } else if (problem == 2) {
                    graphForHighlighting = generateSimpleAdjacencyList(true);
                    AdjacencyList<Integer> graph = generateSimpleAdjacencyList(true);
                    MDS mds = new MDS(graph);

                    SwingWorker<List<Integer>, Void> worker = new SwingWorker<>() {
                        @Override
                        protected List<Integer> doInBackground() {
                            return switch (approach) {
                                case "Força Bruta" -> mds.bruteForceApproach(graph);
                                case "Branch and Bound" -> mds.solveBranchAndBound(new HashSet<>(), new HashSet<>(), new HashSet<>(graph.V()));
                                case "Heurístico (Iterated Greedy Algorithm)" -> mds.iteratedGreedyApproach(graph, 100, 3);
                                default -> throw new IllegalStateException("\"" + approach + "\"" + " não suportada para o Problema 2.");
                            };
                        }

                        @Override
                        protected void done() {
                            try {
                                List<Integer> resultSet = get();
                                Map<Integer, String> stationNameMap = buildStationNameMap();

                                highlightedVertices.clear();
                                highlightedEdges.clear();
                                highlightedVertices.addAll(resultSet);

                                mainPanel.repaint();

                                StringBuilder sb = new StringBuilder("MDS: ");
                                resultSet.forEach(id -> sb.append(stationNameMap.get(id)).append(" -> "));
                                sb.setLength(sb.length() - 4);
                                System.out.println(sb);

                                JOptionPane.showMessageDialog(ProblemSolverDialog.this,
                                        "O Menor Conjunto Dominante possui " + resultSet.size() + " vértices.");
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
        AdjacencyList<Integer> graph = new AdjacencyList<>(useEdgeList);
        for (Line edge : lines) {
            Optional<Station> s1Opt = Station.findByName(stations, edge.s1());
            Optional<Station> s2Opt = Station.findByName(stations, edge.s2());

            if (s1Opt.isEmpty())
                System.out.println(edge.s1() + " não está presente");
            else if (s2Opt.isEmpty())
                System.out.println(edge.s2() + " não está presente");
            else {
                int v = s1Opt.get().id();
                int u = s2Opt.get().id();
                graph.addEdge(v, u);
            }
        }
        return graph;
    }

    List<Station> readStationFile(String filename) {
        List<Station> stationsList = new LinkedList<>();
        int id = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = STATION_LINE_REGEX.matcher(line.trim());
                if (!matcher.matches()) {
                    throw new IllegalStateException("Linha de estações de metro inválida -> " + line);
                }
                stationsList.add(new Station(id++, matcher.group(1),
                        new Cordinates(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)))));
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler as estações do metro =(\n" + e.getMessage());
        }
        return stationsList;
    }

    List<Line> readLineFile(String filename) {
        List<Line> linesList = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher headerMatcher = LINE_LINE_HEADER_REGEX.matcher(line.trim());
                if (!headerMatcher.matches()) {
                    throw new IllegalStateException("Linha header de linha de metro inválida -> " + line);
                }
                String lineName = headerMatcher.group(1);
                Color lineColor = colorMap.get(headerMatcher.group(2).toLowerCase());
                int segmentCount = Integer.parseInt(headerMatcher.group(3));

                for (int i = 0; i < segmentCount && (line = br.readLine()) != null; i++) {
                    Matcher bodyMatcher = LINE_LINE_BODY_REGEX.matcher(line.trim());
                    if (!bodyMatcher.matches()) {
                        throw new IllegalStateException("Linha body de linha de metro inválida -> " + line);
                    }
                    linesList.add(new Line(bodyMatcher.group(1), bodyMatcher.group(2), lineName, lineColor));
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler as linhas do metro =(\n" + e.getMessage());
        }
        return linesList;
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

        void paintEdge(Graphics2D g, Line edge) {
            Optional<Station> s1Opt = Station.findByName(stations, edge.s1());
            Optional<Station> s2Opt = Station.findByName(stations, edge.s2());
            if (s1Opt.isPresent() && s2Opt.isPresent()) {
                Cordinates pos1 = s1Opt.get().position();
                Cordinates pos2 = s2Opt.get().position();

                int v1 = s1Opt.get().id();
                int v2 = s2Opt.get().id();
                List<Integer> edgeVertices = v1 < v2 ? List.of(v1, v2) : List.of(v2, v1);

                if (highlightedEdges.contains(edgeVertices)) {
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(8));
                } else {
                    g.setColor(edge.color());
                    g.setStroke(EDGE_STROKE);
                }

                g.drawLine(pos1.x(), pos1.y(), pos2.x(), pos2.y());
            }
        }

        void paintNode(Graphics2D g, Station station) {
            int cx = CENTER_NODE_POS.apply(station.position().x());
            int cy = CENTER_NODE_POS.apply(station.position().y());

            if (highlightedVertices.contains(station.id())) {
                int radius = NODE_DIAMETER + 8;
                g.setColor(Color.RED);
                g.fillOval(cx - 4, cy - 4, radius, radius);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(3));
                g.drawOval(cx - 4, cy - 4, radius, radius);
            } else {
                g.setColor(NODE_COLOR);
                g.fillOval(cx, cy, NODE_DIAMETER, NODE_DIAMETER);
            }
            paintNodeLabel(g, cx, cy, station.name());
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
