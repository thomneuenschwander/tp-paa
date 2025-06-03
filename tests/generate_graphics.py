import matplotlib.pyplot as plt
import numpy as np

# Dados dos grafos
labels = ['G1', 'G2', 'G3']
vertices = [20, 30, 49]
arestas = [20, 45, 84]

# Posições das barras no eixo x
x = np.arange(len(labels))  # [0, 1, 2]
width = 0.35  # Largura das barras

# Criar a figura e os eixos
fig, ax = plt.subplots(figsize=(10, 6))

# Barras para os vértices (cor verde)
rects1 = ax.bar(x - width/2, vertices, width, label='Vértices', color='green')

# Barras para as arestas (cor azul)
rects2 = ax.bar(x + width/2, arestas, width, label='Arestas', color='blue')

# Adicionar títulos e legendas
ax.set_ylabel('Quantidade')
ax.set_title('Comparação de Vértices e Arestas por Grafo')
ax.set_xticks(x)
ax.set_xticklabels(labels)
ax.legend()

# Adicionar rótulos de valor em cima de cada barra
def autolabel(rects):
    """Anexa um rótulo de texto acima de cada barra em *rects*, exibindo sua altura."""
    for rect in rects:
        height = rect.get_height()
        ax.annotate('{}'.format(height),
                    xy=(rect.get_x() + rect.get_width() / 2, height),
                    xytext=(0, 3),  # Deslocamento vertical de 3 pontos
                    textcoords="offset points",
                    ha='center', va='bottom')

autolabel(rects1)
autolabel(rects2)

# Ajustar layout para evitar que os rótulos se sobreponham
fig.tight_layout()

# Salvar o gráfico
plt.savefig('comparacao_grafos.png')

# Mostrar o gráfico (opcional, dependendo do ambiente de execução)
# plt.show()