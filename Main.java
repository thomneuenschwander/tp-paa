import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
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

record Cordinates(int x, int y) {
}

record Station(String name, Cordinates position) {
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

    List<Station> stations = new ArrayList<>();
    List<Line> lines = new ArrayList<>();
    GraphPanel graphPanel;

    GUI() {
        setTitle("Passe de Metrô de Paris - Solver");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();

        JMenu fMenu = new JMenu("Ler Arquivos");
        JMenuItem fopenStations = new JMenuItem("Ler Estações do Metrô");
        JMenuItem fopenLines = new JMenuItem("Ler Linhas do Metrô");
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

                if (this.graphPanel != null)
                    remove(this.graphPanel);

                this.graphPanel = new GraphPanel(stations, lines);
                JScrollPane scrollPane = new JScrollPane(this.graphPanel);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                add(scrollPane, BorderLayout.CENTER);

                revalidate();
                repaint();
            }
        });

        exit.addActionListener(e -> System.exit(0));

        ActionListener problemaHandler = evt -> {
            if (stations.isEmpty() || lines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "É preciso carregar as entradas de estações e de linhas do metro.", "Nenhum input foi foi fornecido ainda",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

        };

        p1BF.addActionListener(problemaHandler);
        p1BnB.addActionListener(problemaHandler);
        p1Heur.addActionListener(problemaHandler);
        p2Brute.addActionListener(problemaHandler);
        p2BnB.addActionListener(problemaHandler);
        p2Heur.addActionListener(problemaHandler);

        setVisible(true);
    }

    List<Station> readStationFile(String filename) {
        List<Station> stations = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = STATION_LINE_REGEX.matcher(line.trim());
                if (!m.matches())
                    throw new IllegalStateException("Linha de estacoes de metro inválida -> " + line);
                stations.add(new Station(m.group(1),
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
        static final Color NODE_COLOR = Color.BLACK;
        static final int LABEL_PADDING = 4;
        static final BasicStroke EDGE_STROKE = new BasicStroke(4);
        static final Function<Integer, Integer> CENTER_POS = pos -> pos - NODE_DIAMETER / 2;

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
            int cx = CENTER_POS.apply(v.position().x());
            int cy = CENTER_POS.apply(v.position().y());
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
