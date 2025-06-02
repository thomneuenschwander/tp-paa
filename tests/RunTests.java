package tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
public class RunTests {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String problemFlag = "";
        String filePath = "";
        int rootVertex = -1;
        String techniqueFlag = "";
        int argOffset = 0;

        if (args[0].equals("-p")) {
            if (args.length < 2) {
                printUsage();
                return;
            }
            problemFlag = args[1];
            argOffset = 2;
        } else {
            System.err.println("Erro: Flag do problema (-p 1 ou -p 2) deve ser o primeiro argumento.");
            printUsage();
            return;
        }

        if (problemFlag.equals("1")) {
            if (args.length < argOffset + 3) {
                System.err.println("Erro: Argumentos insuficientes para Max Cycle (-p 1).");
                printSpecificUsage(problemFlag);
                return;
            }
            filePath = args[argOffset];
            try {
                rootVertex = Integer.parseInt(args[argOffset + 1]);
            } catch (NumberFormatException e) {
                System.err.println("Erro: Vértice raiz deve ser um inteiro para Max Cycle.");
                printSpecificUsage(problemFlag);
                return;
            }
            techniqueFlag = args[argOffset + 2];
        } else if (problemFlag.equals("2")) {
            if (args.length < argOffset + 2) {
                System.err.println("Erro: Argumentos insuficientes para Minimum Dominating Set (-p 2).");
                printSpecificUsage(problemFlag);
                return;
            }
            filePath = args[argOffset];
            techniqueFlag = args[argOffset + 1];
        } else {
            System.err.println("Erro: Valor inválido para a flag -p. Use '1' para Max Cycle ou '2' para MDS.");
            printUsage();
            return;
        }

        if (!techniqueFlag.equals("-bf") && !techniqueFlag.equals("-bnb") && !techniqueFlag.equals("-heur")) {
            System.err.println("Erro: Flag de técnica inválida '" + techniqueFlag + "'. Use -bf, -bnb, ou -heur.");
            printSpecificUsage(problemFlag);
            return;
        }
        AdjacencyList<Integer> graph;
        if (problemFlag.equals("1")) {
            graph = AdjacencyList.fromFile(filePath, false);
        } else {
            graph = AdjacencyList.fromFile(filePath, true);
        }

        if (graph == null || graph.V().isEmpty()) {
            System.err.println("Erro: Não foi possível carregar o grafo do arquivo ou o grafo está vazio: " + filePath);
            return;
        }

        System.out.println("Grafo carregado de: " + filePath);
        System.out.println("Problema selecionado: " + (problemFlag.equals("1") ? "Max Cycle (Caminho Máximo)"
                : "Minimum Dominating Set (Conjunto Dominante Mínimo)"));
        if (problemFlag.equals("1")) {
            System.out.println("Vértice raiz: " + rootVertex);
        }
        System.out.println("Flag de técnica: " + techniqueFlag);
        System.out.println("---------------------------");

        if (problemFlag.equals("1")) {
            runMaxCycle(graph, rootVertex, techniqueFlag);
        } else { // problemFlag.equals("2")
            runMDS(graph, techniqueFlag);
        }

        System.out.println("---------------------------");
    }

    private static void runMaxCycle(AdjacencyList<Integer> graph, int root, String flag) {
        MaxCycle maxCycleFinder = new MaxCycle(graph);
        List<Integer> cyclePath = Collections.emptyList();
        String techniqueName = "Desconhecida";

        switch (flag) {
            case "-bf":
                techniqueName = "Max Cycle - Força Bruta";
                System.out.println("Executando " + techniqueName + "...");
                cyclePath = maxCycleFinder.bruteForceApproach(root);
                break;
            case "-heur":
                techniqueName = "Max Cycle - Heurística Randomizada";
                System.out.println("Executando " + techniqueName + "...");
                cyclePath = maxCycleFinder.parcialGreedyHeuristicApproach(root);
                break;
            case "-bnb":
                techniqueName = "Max Cycle - Branch and Bound";
                System.out.println("Executando " + techniqueName + "...");
                cyclePath = maxCycleFinder.branchAndBoundApproach(root);
                break;
            default:
                System.err.println("Flag de técnica inválida para Max Cycle: " + flag);
                printSpecificUsage("1");
                return;
        }

        System.out.println("---------------------------");
        if (cyclePath != null && !cyclePath.isEmpty()) {
            System.out.println(techniqueName + " - Tamanho Máximo do Ciclo: " + maxCycleFinder.getMax());
            System.out.println(techniqueName + " - Caminho do Ciclo: " + cyclePath);
        } else {
            System.out.println(techniqueName + ": Nenhum ciclo encontrado passando pela raiz " + root
                    + ", ou raiz inválida (ex: grau < 2 ou não está no grafo).");
            if (maxCycleFinder.getMax() > 0) {
                System.out.println(techniqueName + " - Tamanho Máximo do Ciclo (fallback): " + maxCycleFinder.getMax());
                System.out
                        .println(techniqueName + " - Caminho do Ciclo (fallback): " + maxCycleFinder.getMaxCyclePath());
            }
        }
    }

    private static void runMDS(AdjacencyList<Integer> graph, String flag) {
        MDS mdsFinder = new MDS(graph);
        List<Integer> dominatingSet = Collections.emptyList();
        String techniqueName = "Desconhecida";

        switch (flag) {
            case "-bf":
                techniqueName = "MDS - Força Bruta";
                System.out.println("Executando " + techniqueName + "...");
                dominatingSet = mdsFinder.bruteForceApproach(graph);
                break;
            case "-bnb":
                techniqueName = "MDS - Branch and Bound";
                System.out.println("Executando " + techniqueName + "...");
                dominatingSet = mdsFinder.solveBranchAndBound(new HashSet<>(), new HashSet<>(),
                        new HashSet<>(graph.V()));
                break;
            case "-heur":
                techniqueName = "MDS - Iterated Greedy (Heurística)";
                System.out.println("Executando " + techniqueName + "...");
                int maxIterations = 100;
                int removalSize = Math.max(1, graph.V().size() / 10);
                if (graph.V().isEmpty() && removalSize == 0)
                    removalSize = 1;
                else if (graph.V().size() > 0 && removalSize == 0)
                    removalSize = 1;
                dominatingSet = mdsFinder.iteratedGreedyApproach(graph, maxIterations, removalSize);
                break;
            default:
                System.err.println("Flag de técnica inválida para MDS: " + flag);
                printSpecificUsage("2");
                return;
        }
        System.out.println("---------------------------");
        if (dominatingSet != null && !dominatingSet.isEmpty()) {
            System.out.println(techniqueName + " - Tamanho do Conjunto Dominante: " + dominatingSet.size());

            List<Integer> sortedDominatingSet = new ArrayList<>(dominatingSet);
            Collections.sort(sortedDominatingSet);
            System.out.println(techniqueName + " - Conjunto Dominante: " + sortedDominatingSet);
        } else {
            System.out.println(
                    techniqueName + ": Nenhum conjunto dominante encontrado ou a heurística não produziu resultado.");
        }

        if (flag.equals("-bnb") && mdsFinder.minDominatingSet != null && !mdsFinder.minDominatingSet.isEmpty()) {
            if (dominatingSet == null || dominatingSet.isEmpty()
                    || mdsFinder.minDominatingSet.size() < dominatingSet.size()) {
                List<Integer> bnbInternalResult = new ArrayList<>(mdsFinder.minDominatingSet);
                Collections.sort(bnbInternalResult);
                System.out.println(
                        techniqueName + " (resultado interno atualizado via mdsFinder.minDominatingSet) - Tamanho: "
                                + bnbInternalResult.size());
                System.out.println(
                        techniqueName + " (resultado interno atualizado via mdsFinder.minDominatingSet) - Conjunto: "
                                + bnbInternalResult);
            }
        }
    }

    private static void printUsage() {
        System.err.println("Uso: java tests.RunTests -p <1|2> [opções específicas do problema]");
        System.err.println("  -p 1: Max Cycle (Caminho Máximo em Grafo)");
        System.err.println("  -p 2: Minimum Dominating Set (Conjunto Dominante Mínimo)");
        System.err.println("\nFlags de técnica comuns (para -p 1 e -p 2): -bf, -bnb, -heur");
        printSpecificUsage("1"); // Mostra exemplo para Max Cycle
        printSpecificUsage("2"); // Mostra exemplo para MDS
    }

    private static void printSpecificUsage(String problemType) {
        if (problemType.equals("1")) {
            System.err.println("\nExemplo para Max Cycle (-p 1):");
            System.err.println("  java tests.RunTests -p 1 <filepath> <root_vertex> [-bf|-bnb|-heur]");
        } else if (problemType.equals("2")) {
            System.err.println("\nExemplo para Minimum Dominating Set (-p 2):");
            System.err.println("  java tests.RunTests -p 2 <filepath> [-bf|-bnb|-heur]");
        }
    }
}