package tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Test {
    public static void main(String[] args) {
        final int ROOT = 1;
        final String FILENAME = "./tests/graph_test_n5.txt";

        runPythonCycleScript(FILENAME, ROOT);

        AdjacencyList<Integer> graph = AdjacencyList.fromFile(FILENAME, true);

        System.out.println("Graph:\n" + graph);
        System.out.println("ROOT: " + ROOT);

        MaxCycle maxCycleFinder = new MaxCycle(graph);
        System.out.println("---------------------------");
        List<Integer> maxCycleBF = maxCycleFinder.bruteForceApproach(ROOT);
        System.out.println("Brute Force: " + maxCycleFinder.getMax());
        System.out.println("Cycle: " + maxCycleBF);
        System.out.println("---------------------------");
        List<Integer> maxCycleGreedyHeuristic = maxCycleFinder.parcialGreedyHeuristicApproach(ROOT);
        System.out.println("Heuristic: " + maxCycleFinder.getMax());
        System.out.println("Cycle: " + maxCycleGreedyHeuristic);
        System.out.println("---------------------------");
        List<Integer> maxCycleRadomizedHeuristic = maxCycleFinder.randomizedHeuristicApproach(ROOT);
        System.out.println("Randomized Heuristic: " + maxCycleFinder.getMax());
        System.out.println("Cycle: " + maxCycleRadomizedHeuristic);
    }

    private static void runPythonCycleScript(String filename, int root) {
        Thread pythonThread = new Thread(() -> {
            System.out.println("Thread Python iniciada...");
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", "./tests/networkX.py", filename,
                        String.valueOf(root));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("Erro ao executar script Python:");
                e.printStackTrace();
            }
        });

        pythonThread.start();
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

    // Método de força bruta que testa todos os subconjuntos de vértices para
    // encontrar o menor conjunto dominante
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

            // Verifica se o subconjunto atual é menor que o melhor até agora e se é
            // conjunto dominante válido
            if (currentSet.size() < minSizeFound && isValidDominatingSet(currentSet, graph)) {
                minSizeFound = currentSet.size();
                bestSolution = new ArrayList<>(currentSet);
                System.out
                        .println("Novo menor conjunto dominante: " + bestSolution + " (tamanho: " + minSizeFound + ")");
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

    // Verifica se o conjunto fornecido domina o grafo, ou seja, cada vértice está
    // ou no conjunto ou tem vizinho no conjunto
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
                if (!hasNeighborInSet)
                    return false; // Não é conjunto dominante se algum vértice não estiver coberto

                coveredVertices.add(vertex); // Marca vértice como coberto
            }
        }
        return true;
    }

    // Método principal do branch and bound para conjunto dominante mínimo
    public int branchAndBoundApproach(Set<Integer> currentInSet, Set<Integer> currentOutSet,
            Set<Integer> undecidedVertices) {
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

        // Caso não tenha mais vértices para decidir e ainda não domine o grafo,
        // abandona ramo
        if (undecidedVertices.isEmpty()) {
            return upperBound;
        }

        // Calcula limite inferior para decidir se continua explorando o ramo
        int lowerBound = lowerBound(currentInSet, currentOutSet, undecidedVertices);
        if (lowerBound >= this.upperBound) {
            return upperBound;
        }

        // Escolha do próximo vértice a incluir ou excluir: aquele que cobre maior
        // número de vértices ainda não dominados
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
        if (currentInSet.contains(vertex))
            return true;
        for (int neighbor : this.graph.neighbors(vertex)) {
            if (currentInSet.contains(neighbor))
                return true;
        }
        return false;
    }

    // Confirma se todo o grafo está dominado pelo conjunto atual
    private boolean checkFullyDominated(Set<Integer> currentInSet) {
        for (int vertex : this.graph.V()) {
            if (!isVertexDominated(vertex, currentInSet))
                return false;
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
                if (!canBeCovered)
                    return Integer.MAX_VALUE;
            }
        }

        // Calcula a máxima cobertura de qualquer vértice indeciso sobre vértices não
        // dominados
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

        if (maxCoverage == 0)
            return Integer.MAX_VALUE;

        // Estima quantos vértices adicionais são necessários para cobrir os não
        // dominados
        int minVerticesNeeded = (int) Math.ceil((double) notDominated.size() / maxCoverage);
        return currentSize + minVerticesNeeded;
    }

    // Inicializa as variáveis para rodar o Branch and Bound
    public List<Integer> solveBranchAndBound(Set<Integer> currentInSet, Set<Integer> currentOutSet,
            Set<Integer> undecidedVertices) {
        this.upperBound = this.graph.V().size() + 1; // Valor inicial alto para upperBound
        this.minDominatingSet = new HashSet<>();
        branchAndBoundApproach(currentInSet, currentOutSet, undecidedVertices);
        return new ArrayList<>(this.minDominatingSet);
    }

    // Heurística iterated greedy: tenta melhorar solução inicial removendo e
    // readicionando vértices
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        adjacency.keySet().forEach(v -> sb.append(v).append(": ").append(adjacency.get(v)).append("\n"));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }


    public static AdjacencyList<Integer> fromFile(String filename, boolean useEdgeList) {
        AdjacencyList<Integer> graph = new AdjacencyList<>(useEdgeList);

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(br.readLine());

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                int u = Integer.parseInt(parts[0]);
                int v = Integer.parseInt(parts[1]);
                if (u > n || v > n) {
                    System.err.println("Invalid edge: " + u + " - " + v);
                    continue;
                }
                graph.addEdge(u, v);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return graph;
    }
}