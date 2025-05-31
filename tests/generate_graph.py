import random

# ====== CONFIGURAÇÃO ======
n = 15               # número de vértices
density = 0.3        # densidade do grafo (entre 0 e 1)
output_file = "./tests/graph_random.txt"
# ==========================

if not (0 < density <= 1):
    raise ValueError("Density must be between 0 (exclusive) and 1 (inclusive)")

# número máximo de arestas possíveis sem laços
max_possible_edges = n * (n - 1) // 2
target_edges = int(max_possible_edges * density)

edges = set()
while len(edges) < target_edges:
    u = random.randint(0, n - 1)
    v = random.randint(0, n - 1)
    if u != v:
        edge = tuple(sorted((u, v)))
        edges.add(edge)

# salvar no arquivo
with open(output_file, "w") as f:
    f.write(f"{n}\n")
    for u, v in edges:
        f.write(f"{u} {v}\n")

print(f"Grafo gerado com sucesso: {n} vértices, {len(edges)} arestas → {output_file}")
