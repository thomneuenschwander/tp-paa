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
        P.add(s);
        bruteForce(P, s, s);

        return C;
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
        return C;
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
    private final AdjacencyList<Integer> graph; // Grafo para o cálculo do conjunto dominante mínimo
    public Set<Integer> minDominatingSet; // Menor conjunto dominante encontrado até o momento
    private int upperBound; // Limite superior para poda no algoritmo branch and bound

    public MDS(AdjacencyList<Integer> graph) {
        this.graph = graph;
        this.minDominatingSet = new HashSet<>();
    }

    // Método de força bruta que testa todos os subconjuntos de vértices para encontrar o menor conjunto dominante
    public List<Integer> bruteForceApproach(AdjacencyList<Integer> graph) {
        Set<Integer> allVertices = graph.V();
        int numVertices = allVertices.size();
        List<Integer> vertexList = new ArrayList<>(allVertices);

        List<Integer> bestSolution = null;
        int minSizeFound = numVertices + 1;

        System.out.println("Grafo possui " + numVertices + " vértices");

        long maxSubsets = 1L << numVertices; // Calcula 2^n subconjuntos possíveis
        for (long mask = 0; mask < maxSubsets; mask++) {
            List<Integer> currentSet = new ArrayList<>();
            for (int i = 0; i < numVertices; i++) {
                // Verifica se o bit i está ativo no subconjunto atual
                if ((mask & (1L << i)) != 0) {
                    currentSet.add(vertexList.get(i));
                }
            }

            // Verifica se o subconjunto atual é menor que o melhor até agora e se é conjunto dominante válido
            if (currentSet.size() < minSizeFound && isValidDominatingSet(currentSet, graph)) {
                minSizeFound = currentSet.size();
                bestSolution = new ArrayList<>(currentSet);
                System.out.println("Novo menor conjunto dominante: " + bestSolution + " (tamanho: " + minSizeFound + ")");
            }
        }

        // Caso não encontre nenhum conjunto dominante, retorna lista vazia
        if (bestSolution == null) {
            System.out.println("Nenhum conjunto dominante encontrado, retornando lista vazia");
            return new ArrayList<>();
        }

        System.out.println("Conjunto dominante mínimo final: " + bestSolution + " (tamanho: " + minSizeFound + ")");
        return bestSolution;
    }

    // Verifica se o conjunto fornecido domina o grafo, ou seja, cada vértice está ou no conjunto ou tem vizinho no conjunto
    public boolean isValidDominatingSet(List<Integer> subset, AdjacencyList<Integer> graph) {
        Set<Integer> coveredVertices = new HashSet<>(subset);

        for (Integer vertex : graph.V()) {
            // Se o vértice ainda não está coberto
            if (!coveredVertices.contains(vertex)) {
                boolean hasNeighborInSet = false;
                // Verifica se algum vizinho do vértice está no conjunto dominante
                for (Integer neighbor : graph.neighbors(vertex)) {
                    if (subset.contains(neighbor)) {
                        hasNeighborInSet = true;
                        break;
                    }
                }
                if (!hasNeighborInSet) return false; // Não é conjunto dominante se algum vértice não estiver coberto

                coveredVertices.add(vertex); // Marca vértice como coberto
            }
        }
        return true;
    }

    // Método principal do branch and bound para conjunto dominante mínimo
    public int branchAndBoundApproach(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        // Verifica se conjunto atual domina todo o grafo
        if (checkFullyDominated(currentInSet)) {
            // Atualiza solução ótima se for menor que o melhor atual
            if (currentInSet.size() < this.upperBound) {
                this.upperBound = currentInSet.size();
                this.minDominatingSet = new HashSet<>(currentInSet);
                System.out.println("MDS: " + this.minDominatingSet + ", Size: " + this.upperBound);
            }
            return currentInSet.size();
        }

        // Caso não tenha mais vértices para decidir e ainda não domine o grafo, abandona ramo
        if (undecidedVertices.isEmpty()) {
            return upperBound;
        }

        // Calcula limite inferior para decidir se continua explorando o ramo
        int lowerBound = lowerBound(currentInSet, currentOutSet, undecidedVertices);
        if (lowerBound >= this.upperBound) {
            return upperBound;
        }

        // Escolha do próximo vértice a incluir ou excluir: aquele que cobre maior número de vértices ainda não dominados
        int nextVertex = -1;
        int maxCoverage = -1;

        Set<Integer> currentlyUndominated = new HashSet<>();
        for (int v : this.graph.V()) {
            if (!isVertexDominated(v, currentInSet)) {
                currentlyUndominated.add(v);
            }
        }

        // Avalia cada vértice indeciso para decidir próximo passo
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

        // Divide o problema em dois ramos: incluir e não incluir o vértice escolhido
        Set<Integer> remainingUndecided = new HashSet<>(undecidedVertices);
        remainingUndecided.remove(nextVertex);

        Set<Integer> inSetBranch = new HashSet<>(currentInSet);
        inSetBranch.add(nextVertex);
        int branchInResult = branchAndBoundApproach(inSetBranch, currentOutSet, remainingUndecided);

        Set<Integer> outSetBranch = new HashSet<>(currentOutSet);
        outSetBranch.add(nextVertex);
        int branchOutResult = branchAndBoundApproach(currentInSet, outSetBranch, remainingUndecided);

        // Retorna o melhor resultado entre os dois ramos
        return Math.min(branchInResult, branchOutResult);
    }

    // Verifica se vértice está dominado pelo conjunto atual (ele mesmo ou vizinhos)
    private boolean isVertexDominated(int vertex, Set<Integer> currentInSet) {
        if (currentInSet.contains(vertex)) return true;
        for (int neighbor : this.graph.neighbors(vertex)) {
            if (currentInSet.contains(neighbor)) return true;
        }
        return false;
    }

    // Confirma se todo o grafo está dominado pelo conjunto atual
    private boolean checkFullyDominated(Set<Integer> currentInSet) {
        for (int vertex : this.graph.V()) {
            if (!isVertexDominated(vertex, currentInSet)) return false;
        }
        return true;
    }

    // Calcula limite inferior para podar ramos que não podem melhorar solução atual
    private int lowerBound(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        int currentSize = currentInSet.size();

        Set<Integer> notDominated = new HashSet<>();
        for (int v : this.graph.V()) {
            if (!isVertexDominated(v, currentInSet)) {
                notDominated.add(v);
            }
        }

        // Verifica se algum vértice não dominado está no conjunto excluído
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

        // Calcula a máxima cobertura de qualquer vértice indeciso sobre vértices não dominados
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

        // Estima quantos vértices adicionais são necessários para cobrir os não dominados
        int minVerticesNeeded = (int) Math.ceil((double) notDominated.size() / maxCoverage);
        return currentSize + minVerticesNeeded;
    }

    // Inicializa as variáveis para rodar o Branch and Bound
    public List<Integer> solveBranchAndBound(Set<Integer> currentInSet, Set<Integer> currentOutSet, Set<Integer> undecidedVertices) {
        this.upperBound = this.graph.V().size() + 1; // Valor inicial alto para upperBound
        this.minDominatingSet = new HashSet<>();
        branchAndBoundApproach(currentInSet, currentOutSet, undecidedVertices);
        return new ArrayList<>(this.minDominatingSet);
    }

    // Heurística iterated greedy: tenta melhorar solução inicial removendo e readicionando vértices
    public List<Integer> iteratedGreedyApproach(AdjacencyList<Integer> graph, int maxIterations, int removalSize) {
        Random random = new Random();

        Set<Integer> solution = new HashSet<>();
        List<List<Integer>> edges = graph.getEdges();
        Set<List<Integer>> uncoveredEdges = new HashSet<>(edges);

        // Construção inicial: seleciona vértices até cobrir todas as arestas
        while (!uncoveredEdges.isEmpty()) {
            Map<Integer, Integer> coverageCount = new HashMap<>();
            for (List<Integer> edge : uncoveredEdges) {
                int u = edge.get(0), v = edge.get(1);
                coverageCount.put(u, coverageCount.getOrDefault(u, 0) + 1);
                coverageCount.put(v, coverageCount.getOrDefault(v, 0) + 1);
            }

            // Escolhe vértice que cobre mais arestas descobertas
            int bestVertex = coverageCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get()
                    .getKey();

            solution.add(bestVertex);
            uncoveredEdges.removeIf(edge -> edge.contains(bestVertex));
        }

        List<Integer> bestSolution = new ArrayList<>(solution);

        // Fase iterativa: destrói e reconstrói a solução para tentar melhorá-la
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Remove vértices aleatoriamente (fase destruição)
            List<Integer> partialSolution = new ArrayList<>(solution);
            for (int i = 0; i < removalSize && !partialSolution.isEmpty(); i++) {
                int indexToRemove = random.nextInt(partialSolution.size());
                partialSolution.remove(indexToRemove);
            }

            // Reconstrói solução para cobrir todas as arestas restantes
            Set<Integer> rebuiltSolution = new HashSet<>(partialSolution);
            Set<List<Integer>> uncovered = new HashSet<>();
            for (List<Integer> edge : edges) {
                if (!rebuiltSolution.contains(edge.get(0)) && !rebuiltSolution.contains(edge.get(1))) {
                    uncovered.add(edge);
                }
            }

            // Adiciona vértices para cobrir todas as arestas descobertas
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

            // Atualiza melhor solução se a reconstruída for melhor
            if (solution.size() < bestSolution.size()) {
                bestSolution = new ArrayList<>(solution);
            }
        }

        System.out.println("Melhor solução encontrada: " + bestSolution + " (Tamanho: " + bestSolution.size() + ")");
        return bestSolution;
    }
}

class AdjacencyList<T> {
    private final Map<T, Set<T>> adjacency; // Mapa que armazena os vizinhos de cada vértice
    private final boolean useEdgeList; // Flag para controle se lista de arestas será mantida
    private List<List<T>> edges; // Lista explícita de arestas
    private int numEdges; // Contador de número de arestas

    // Retorna o número atual de arestas
    public int getNumEdges() {
        return numEdges;
    }

    // Construtor padrão sem uso de lista de arestas
    public AdjacencyList() {
        this.adjacency = new HashMap<>();
        this.useEdgeList = false;
    }

    // Construtor que permite ativar lista de arestas
    public AdjacencyList(boolean useEdgeList) {
        this.useEdgeList = useEdgeList;
        this.adjacency = new HashMap<>();
        this.edges = new ArrayList<>();
        this.numEdges = 0;
    }

    // Retorna conjunto de vizinhos diretos de um vértice, vazio se vértice não existe
    public Set<T> neighbors(T vertex) {
        return adjacency.getOrDefault(vertex, Collections.emptySet());
    }

    // Retorna conjunto de vizinhos + o próprio vértice (fechamento)
    public Set<T> closedNeighbors(T vertex) {
        Set<T> closed = new HashSet<>(neighbors(vertex));
        closed.add(vertex);
        return closed;
    }

    // Retorna conjunto de todos os vértices do grafo
    public Set<T> V() {
        return adjacency.keySet();
    }

    // Adiciona uma aresta entre v e u, criando os vértices se necessário
    public boolean addEdge(T v, T u) {
        adjacency.putIfAbsent(v, new HashSet<>());
        adjacency.putIfAbsent(u, new HashSet<>());

        boolean addedV = adjacency.get(v).add(u);
        boolean addedU = adjacency.get(u).add(v);

        // Se uso de lista explícita de arestas estiver ativado, adiciona a aresta nela
        if (useEdgeList) {
            edges.add(List.of(v, u));
            numEdges++;
        }

        return addedV || addedU; // Retorna true se alguma aresta foi efetivamente adicionada
    }

    // Retorna o grau (número de vizinhos) de um vértice
    public int degree(T vertex) {
        return neighbors(vertex).size();
    }

    // Clona o grafo atual criando uma cópia independente da estrutura de adjacência
    @Override
    public AdjacencyList<T> clone() {
        AdjacencyList<T> newGraph = new AdjacencyList<>();
        for (Map.Entry<T, Set<T>> entry : this.adjacency.entrySet()) {
            newGraph.adjacency.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        newGraph.numEdges = this.numEdges;
        return newGraph;
    }

    // Retorna a lista explícita de arestas (se ativada), ou lança exceção
    public List<List<T>> getEdges() {
        if (useEdgeList) return edges;
        throw new UnsupportedOperationException("Edge list is not enabled.");
    }

    // Remove um vértice do grafo, removendo também suas arestas incidentes
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
                    numEdges--; // Atualiza contador de arestas
                }
            }
        }

        adjacency.remove(vertex); // Remove o vértice do mapa
        return true;
    }

    // Remove múltiplos vértices; retorna false se alguma remoção falhar
    public boolean removeMultipleVertex(Set<T> vertices) {
        for (T vertex : vertices) {
            if (!removeVertex(vertex)) {
                return false;
            }
        }
        return true;
    }

    // Imprime o grafo no console mostrando cada vértice e seus vizinhos
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

record Cordinates(int x, int y) {}  // Representa coordenadas (x, y)

record Station(int id, String name, Cordinates position) {
    // Busca uma estação pelo nome na lista fornecida
    static Optional<Station> findByName(List<Station> stations, String name) {
        return stations.stream().filter(s -> s.name.equals(name)).findFirst();
    }
}

record Line(String s1, String s2, String name, Color color) {}  // Representa uma linha entre duas estações com nome e cor

class GUI extends JFrame {
    static final int FRAME_WIDTH = 800; // Largura da janela
    static final int FRAME_HEIGHT = 600; // Altura da janela

    // Expressões regulares para ler arquivos
    static final Pattern STATION_LINE_REGEX = Pattern.compile("\"([^\"]+)\"\\s+(\\d+)\\s+(\\d+)");
    static final Pattern LINE_LINE_HEADER_REGEX = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\d+)");
    static final Pattern LINE_LINE_BODY_REGEX = Pattern.compile("\"([^\"]+)\"\\s+\"([^\"]+)\"");

    Set<Integer> highlightedVertices = new HashSet<>(); // Vértices destacados (ex: caminho encontrado)
    Set<List<Integer>> highlightedEdges = new HashSet<>(); // Arestas destacadas no grafo
    AdjacencyList<Integer> graphForHighlighting; // Grafo para destaque visual

    // Mapa de nomes para cores das linhas do metrô
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

    JPanel mainPanel; // Painel principal que exibirá o grafo
    List<Station> stations = new ArrayList<>(); // Lista das estações carregadas
    List<Line> lines = new ArrayList<>(); // Lista das linhas carregadas

    public GUI() {
        setTitle("Passe de Metro de Paris - Solver"); // Define título da janela
        setSize(FRAME_WIDTH, FRAME_HEIGHT); // Define tamanho da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Fecha app quando janela fechada
        setLayout(new BorderLayout()); // Layout principal da janela

        mainPanel = new JPanel(new BorderLayout()); // Inicializa painel para conteúdo principal
        add(mainPanel, BorderLayout.CENTER); // Adiciona painel ao centro da janela

        JMenuBar menuBar = new JMenuBar(); // Cria barra de menus

        // Menu para carregamento de arquivos
        JMenu fileMenu = new JMenu("Ler Arquivos");
        JMenuItem openStations = new JMenuItem("Ler Estações do Metro");
        JMenuItem openLines = new JMenuItem("Ler Linhas do Metro");
        JMenuItem exitItem = new JMenuItem("Sair");

        // Adiciona itens ao menu arquivos
        fileMenu.add(openStations);
        fileMenu.add(openLines);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Menu para resoluções do problema 1
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

        // Menu para resoluções do problema 2
        JMenu problem2Menu = new JMenu("Problema 2");
        JMenuItem p2BruteForce = new JMenuItem("Força Bruta");
        JMenuItem p2BranchAndBound = new JMenuItem("Branch and Bound");
        JMenuItem p2IteratedGreedy = new JMenuItem("Heurístico (Iterated Greedy Algorithm)");

        problem2Menu.add(p2BruteForce);
        problem2Menu.add(p2BranchAndBound);
        problem2Menu.add(p2IteratedGreedy);
        menuBar.add(problem2Menu);

        setJMenuBar(menuBar); // Define barra de menus da janela

        // Ações para carregar arquivo de estações
        openStations.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(".")); // Diretório atual
            chooser.setFileFilter(new FileFilterImpl("_stations.txt")); // Filtra arquivos
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                stations = readStationFile(chooser.getSelectedFile().getAbsolutePath()); // Lê arquivo
                JOptionPane.showMessageDialog(this, stations.size() + " estações carregadas"); // Mensagem sucesso
            }
        });

        // Ações para carregar arquivo de linhas
        openLines.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setFileFilter(new FileFilterImpl("_lines.txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                lines = readLineFile(chooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, lines.size() + " linhas carregadas");

                GraphPanel graphPanel = new GraphPanel(stations, lines); // Cria painel do grafo
                JScrollPane scrollPane = new JScrollPane(graphPanel); // Painel com scroll para o grafo
                scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Ajusta scroll vertical

                mainPanel.removeAll(); // Limpa conteúdo anterior
                mainPanel.add(scrollPane, BorderLayout.CENTER); // Adiciona grafo ao painel principal
                mainPanel.revalidate(); // Atualiza layout
                mainPanel.repaint(); // Repinta tela
            }
        });

        exitItem.addActionListener(e -> System.exit(0)); // Sai do programa

        // Ações dos botões do problema 1, abre diálogo de solução
        p1BruteForce.addActionListener(e -> new ProblemSolverDialog(this, 1, p1BruteForce.getText()));
        p1BranchAndBound.addActionListener(e -> new ProblemSolverDialog(this, 1, p1BranchAndBound.getText()));
        p1GreedyHeuristic.addActionListener(e -> new ProblemSolverDialog(this, 1, p1GreedyHeuristic.getText()));
        p1RandomHeuristic.addActionListener(e -> new ProblemSolverDialog(this, 1, p1RandomHeuristic.getText()));

        // Ações dos botões do problema 2, abre diálogo de solução
        p2BruteForce.addActionListener(e -> new ProblemSolverDialog(this, 2, p2BruteForce.getText()));
        p2BranchAndBound.addActionListener(e -> new ProblemSolverDialog(this, 2, p2BranchAndBound.getText()));
        p2IteratedGreedy.addActionListener(e -> new ProblemSolverDialog(this, 2, p2IteratedGreedy.getText()));

        setVisible(true); // Exibe a janela
    }

    // Mapeia id da estação para seu nome para facilitar exibição
    private Map<Integer, String> buildStationNameMap() {
        Map<Integer, String> stationNameMap = new HashMap<>();
        for (Station s : stations) {
            stationNameMap.put(s.id(), s.name());
        }
        return stationNameMap;
    }

    // Diálogo para entrada e execução das soluções dos problemas
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

            // Contextualização e objetivo para cada problema
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

            if (problem == 1) { // Apenas problema 1 pede estação raiz
                content.add(stationLabel);
                content.add(stationInput);
            }

            JButton runButton = new JButton("Executar");
            content.add(runButton);

            add(content, BorderLayout.CENTER);

            // Quando botão executar for clicado
            runButton.addActionListener(e -> {
                // Verifica se arquivos foram carregados
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
                            // Executa algoritmo escolhido em background para não travar UI
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
                                sb.setLength(sb.length() - 4); // Remove último " -> "
                                System.out.println(sb);

                                int cycleSize = maxCycle.getMax();

                                highlightedVertices.clear();
                                highlightedEdges.clear();

                                if (!resultPath.isEmpty()) {
                                    highlightedVertices.add(resultPath.get(0)); // Destaca vértice raiz
                                    for (int i = 0; i < resultPath.size() - 1; i++) {
                                        int v1 = resultPath.get(i);
                                        int v2 = resultPath.get(i + 1);
                                        List<Integer> edge = v1 < v2 ? List.of(v1, v2) : List.of(v2, v1);
                                        highlightedEdges.add(edge); // Destaca as arestas do caminho
                                    }
                                }

                                mainPanel.repaint(); // Atualiza visualização

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
                            // Executa algoritmo escolhido em background para problema 2
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
                                highlightedVertices.addAll(resultSet); // Destaca o conjunto dominante

                                mainPanel.repaint();

                                StringBuilder sb = new StringBuilder("MDS: ");
                                resultSet.forEach(id -> sb.append(stationNameMap.get(id)).append(" -> "));
                                sb.setLength(sb.length() - 4); // Remove último " -> "
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

    // Gera grafo simples a partir das linhas e estações carregadas
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

    // Lê arquivo de estações e converte para lista de Station
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

    // Lê arquivo de linhas e converte para lista de Line
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

    // Filtro para o JFileChooser baseado no sufixo
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

    // Painel customizado para desenhar o grafo
    class GraphPanel extends JPanel {
        static final Dimension GRAPH_SIZE = new Dimension(1600, 1600); // Tamanho virtual do grafo

        static final int NODE_DIAMETER = 12; // Diâmetro do nó
        static final Function<Integer, Integer> CENTER_NODE_POS = pos -> pos - NODE_DIAMETER / 2; // Centraliza nó
        static final Color NODE_COLOR = Color.BLACK; // Cor padrão dos nós
        static final BasicStroke EDGE_STROKE = new BasicStroke(4); // Espessura padrão das arestas

        List<Station> stations;
        List<Line> lines;

        GraphPanel(List<Station> stations, List<Line> lines) {
            this.stations = stations;
            this.lines = lines;
            setBackground(Color.WHITE); // Fundo branco
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Antialias

            lines.forEach(l -> paintEdge(g2d, l)); // Desenha arestas
            stations.forEach(s -> paintNode(g2d, s)); // Desenha nós
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
                    g.setStroke(new BasicStroke(8)); // Destaque linha grossa vermelha
                } else {
                    g.setColor(edge.color());
                    g.setStroke(EDGE_STROKE); // Linha padrão
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
                g.fillOval(cx - 4, cy - 4, radius, radius); // Nó destacado maior e vermelho
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(3));
                g.drawOval(cx - 4, cy - 4, radius, radius); // Contorno preto
            } else {
                g.setColor(NODE_COLOR);
                g.fillOval(cx, cy, NODE_DIAMETER, NODE_DIAMETER); // Nó padrão
            }
            paintNodeLabel(g, cx, cy, station.name()); // Label estação
        }

        void paintNodeLabel(Graphics2D g, int cx, int cy, String label) {
            int textWidth = label.length();
            int textX = cx - textWidth;
            int textY = cy + 2 * NODE_DIAMETER;
            g.drawString(label, textX, textY); // Desenha o nome da estação próximo ao nó
        }

        @Override
        public Dimension getPreferredSize() {
            return GRAPH_SIZE; // Tamanho preferido do painel (pode dar scroll)
        }
    }
}
