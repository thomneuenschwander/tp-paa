package tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Test {
    public static void main(String[] args) {
        final int ROOT = 12;
        final String FILENAME = "./tests/graph_random.txt";

        runPythonCycleScript(FILENAME, ROOT);

        AdjacencyList<Integer> graph = AdjacencyList.fromFile(FILENAME);

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

    public static AdjacencyList<Integer> fromFile(String filename) {
        AdjacencyList<Integer> graph = new AdjacencyList<>();

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        adj.keySet().forEach(v -> sb.append(v).append(": ").append(adj.get(v)).append("\n"));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}