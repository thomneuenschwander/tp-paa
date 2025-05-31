import networkx as nx
import matplotlib.pyplot as plt
import sys

def read_graph(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
        n = int(lines[0])
        edges = [tuple(map(int, line.strip().split())) for line in lines[1:]]
    G = nx.Graph()
    G.add_nodes_from(range(n))
    G.add_edges_from(edges)
    return G

def draw_graph(G):
    pos = nx.spring_layout(G, seed=42)
    nx.draw(G, pos, with_labels=True, node_color='lightblue', node_size=800, font_weight='bold')
    plt.show()

def longest_simple_cycle_from_node(G, start):
    max_path = []
    visited = set()

    def dfs(current, path):
        nonlocal max_path
        if len(path) > 1 and current == start:
            if len(path) > len(max_path):
                max_path = path[:]
            return

        for neighbor in G.neighbors(current):
            if neighbor not in path or (neighbor == start and len(path) >= 3):
                dfs(neighbor, path + [neighbor])

    dfs(start, [start])
    return max_path

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Uso: python script.py <arquivo.txt> <nó_inicial>")
        sys.exit(1)

    filename = sys.argv[1]
    start_node = int(sys.argv[2])

    G = read_graph(filename)
    draw_graph(G)

    cycle = longest_simple_cycle_from_node(G, start_node)
    print(f"Maior ciclo a partir de {start_node}: {cycle}")
