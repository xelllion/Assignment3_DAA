import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import javax.imageio.ImageIO;

public class PlotGenerator {
    private static final Color[] COLORS = {new Color(24, 95, 180), new Color(210, 78, 28),
            new Color(0, 128, 105), new Color(147, 63, 163), new Color(172, 119, 0), new Color(52, 66, 87)};

    private static class Point {
        int vertices;
        int edges;
        double milliseconds;
    }

    public static void main(String[] args) throws Exception {
        Path table = Path.of(args.length > 0 ? args[0] : "results/tables/summary.csv");
        Path output = Path.of(args.length > 1 ? args[1] : "results/plots");
        Files.createDirectories(output);
        Map<String, List<Point>> series = new LinkedHashMap<>();
        List<String> rows = Files.readAllLines(table, StandardCharsets.UTF_8);
        for (int i = 1; i < rows.size(); i++) {
            String[] fields = rows.get(i).split(",");
            Point point = new Point();
            point.vertices = Integer.parseInt(fields[2]);
            point.edges = Integer.parseInt(fields[3]);
            point.milliseconds = Double.parseDouble(fields[6]);
            if (point.milliseconds <= 0) throw new IllegalArgumentException("Log plot requires positive times");
            series.computeIfAbsent(fields[0], key -> new ArrayList<>()).add(point);
        }
        if (series.size() != 6) throw new IllegalArgumentException("Expected all six benchmark series");
        draw(series, true, output.resolve("time_vs_vertices.png"));
        draw(series, false, output.resolve("time_vs_edges.png"));
        System.out.println("Created two PNG plots from " + table);
    }

    private static void draw(Map<String, List<Point>> series, boolean vertices, Path file) throws Exception {
        int width = 1400, height = 820;
        int left = 125, top = 125, plotWidth = 940, plotHeight = 520;
        double minimum = Double.POSITIVE_INFINITY, maximum = 0;
        for (List<Point> points : series.values()) for (Point p : points) {
            minimum = Math.min(minimum, p.milliseconds); maximum = Math.max(maximum, p.milliseconds);
        }
        int lowPower = (int) Math.floor(Math.log10(minimum));
        int highPower = (int) Math.ceil(Math.log10(maximum));
        if (lowPower == highPower) highPower++;
        int[] ticks = vertices ? new int[]{10, 100, 1000} : new int[]{30, 500, 5000};
        double minX = Math.log10(ticks[0]), maxX = Math.log10(ticks[2]);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
        g.setColor(new Color(28, 44, 61));
        g.setFont(new Font("SansSerif", Font.BOLD, 29));
        g.drawString("Graph algorithms: execution time vs. " + (vertices ? "vertices" : "edges"), 70, 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Mean of 5 measured runs after 5 warmups | seed 42 | both axes use a log scale", 70, 82);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        for (int exponent = lowPower; exponent <= highPower; exponent++) {
            int y = top + plotHeight - (exponent - lowPower) * plotHeight / (highPower - lowPower);
            g.setColor(new Color(221, 227, 233)); g.drawLine(left, y, left + plotWidth, y);
            g.setColor(Color.DARK_GRAY);
            String label = String.format(Locale.ROOT, "%." + Math.max(0, -exponent) + "f", Math.pow(10, exponent));
            g.drawString(label, left - 15 - g.getFontMetrics().stringWidth(label), y + 6);
        }
        for (int tick : ticks) {
            int x = left + (int) ((Math.log10(tick) - minX) / (maxX - minX) * plotWidth);
            g.setColor(new Color(221, 227, 233)); g.drawLine(x, top, x, top + plotHeight);
            g.setColor(Color.DARK_GRAY);
            String label = Integer.toString(tick);
            g.drawString(label, x - g.getFontMetrics().stringWidth(label) / 2, top + plotHeight + 30);
        }
        g.setColor(Color.DARK_GRAY);
        g.drawLine(left, top, left, top + plotHeight);
        g.drawLine(left, top + plotHeight, left + plotWidth, top + plotHeight);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(vertices ? "Number of vertices (V)" : "Number of edges (E)", 470, 706);
        g.rotate(-Math.PI / 2); g.drawString("Average execution time (ms)", -540, 35); g.rotate(Math.PI / 2);
        int index = 0;
        for (Map.Entry<String, List<Point>> entry : series.entrySet()) {
            g.setColor(COLORS[index]); g.setStroke(new BasicStroke(3));
            int lastX = -1, lastY = -1;
            for (Point p : entry.getValue()) {
                int x = left + (int) ((Math.log10(vertices ? p.vertices : p.edges) - minX) / (maxX - minX) * plotWidth);
                int y = top + plotHeight - (int) ((Math.log10(p.milliseconds) - lowPower) / (highPower - lowPower) * plotHeight);
                if (lastX >= 0) g.drawLine(lastX, lastY, x, y);
                g.fillOval(x - 5, y - 5, 10, 10);
                lastX = x; lastY = y;
            }
            int legendY = top + 35 + index * 45;
            g.drawLine(1100, legendY, 1130, legendY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString(entry.getKey(), 1142, legendY + 6);
            index++;
        }
        g.setColor(new Color(70, 80, 90)); g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.drawString("V and E change together: these plots do not isolate their separate effects.", 125, 758);
        g.drawString("Connected undirected graphs for MST; directed graphs for the other algorithms. Times include counters.", 125, 789);
        g.dispose();
        if (!ImageIO.write(image, "png", file.toFile())) throw new IllegalStateException("PNG writer unavailable");
    }
}
