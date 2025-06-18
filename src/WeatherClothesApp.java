import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.json.*;

public class WeatherClothesApp extends JFrame {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JTextArea resultArea, closetArea;
    private JComboBox<String> regionCombo;
    private JTextField dateField;
    private JTextField nameField, typeField, minTempField, maxTempField;
    private ArrayList<ClothingItem> closet;
    private static final String DATA_FILE = "clothes.dat";
    private double temperature = 15.0;
    private String condition = "맑음";

    private static final String SERVICE_KEY = "pTiK74yKsOYli6To5WeFFOH2OCyUIDb0b4+g5876qCO6Y49nutmaLBqyfnY8Rk9PIxzwX2hjJ0RmEmMU+cbNjw==";
    private static final String ULTRA_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";
    private static final String VILLAGE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");
    private static final Color SOFT_BLUE = new Color(200, 220, 255);

    public WeatherClothesApp() {
        super("날씨 기반 옷 추천 앱");
        setSize(420, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        closet = loadClothes();

        JPanel weatherPanel = new JPanel(new GridLayout(3, 2, 2, 2));
        weatherPanel.setBorder(BorderFactory.createTitledBorder("날씨 정보 입력"));
        weatherPanel.setBackground(SOFT_BLUE);
        regionCombo = new JComboBox<>(new String[]{"서울", "부산", "대구", "광주", "대전"});
        dateField = new JTextField(DATE_FMT.format(LocalDate.now()));
        weatherPanel.add(new JLabel("지역 선택:"));
        weatherPanel.add(regionCombo);
        weatherPanel.add(new JLabel("날짜 입력(yyyyMMdd):"));
        weatherPanel.add(dateField);

        JButton realtimeBtn = new RoundButton("실시간 관측 불러오기");
        realtimeBtn.addActionListener(e -> {
            dateField.setText(DATE_FMT.format(LocalDate.now()));
            doFetchAndRecommend();
        });
        weatherPanel.add(new JLabel());
        weatherPanel.add(realtimeBtn);
        add(weatherPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(createMainPanel(), "main");
        contentPanel.add(createAddPanel(), "add");
        contentPanel.add(createClosetPanel(), "closet");
        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "main");
        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(SOFT_BLUE);
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        p.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JButton fetchBtn = new RoundButton("날씨 조회 & 추천");
        fetchBtn.addActionListener(e -> doFetchAndRecommend());
        p.add(fetchBtn, BorderLayout.WEST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(SOFT_BLUE);
        JButton addBtn = new RoundButton("옷 추가");
        addBtn.addActionListener(e -> cardLayout.show(contentPanel, "add"));
        bottom.add(addBtn);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createAddPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(SOFT_BLUE);
        JPanel form = new JPanel(new GridLayout(4, 2, 2, 2));
        form.setBorder(BorderFactory.createTitledBorder("옷 추가"));
        form.setBackground(SOFT_BLUE);
        nameField = new JTextField();
        typeField = new JTextField();
        minTempField = new JTextField();
        maxTempField = new JTextField();
        form.add(new JLabel("이름:")); form.add(nameField);
        form.add(new JLabel("종류:")); form.add(typeField);
        form.add(new JLabel("최저온도:")); form.add(minTempField);
        form.add(new JLabel("최고온도:")); form.add(maxTempField);
        p.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setBackground(SOFT_BLUE);

        JButton saveBtn = new RoundButton("저장");
        saveBtn.addActionListener(e -> {
            try {
                ClothingItem ci = new ClothingItem(
                        nameField.getText().trim(),
                        typeField.getText().trim(),
                        Integer.parseInt(minTempField.getText().trim()),
                        Integer.parseInt(maxTempField.getText().trim())
                );
                closet.add(ci);
                saveClothes();
                JOptionPane.showMessageDialog(this, "옷 저장 완료!");
                nameField.setText(""); typeField.setText("");
                minTempField.setText(""); maxTempField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "입력값을 확인하세요.");
            }
        });

        JButton closetBtn = new RoundButton("내 옷장");
        closetBtn.addActionListener(e -> {
            updateClosetPanel();
            cardLayout.show(contentPanel, "closet");
        });

        bottom.add(saveBtn, BorderLayout.WEST);
        bottom.add(closetBtn, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createClosetPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(SOFT_BLUE);
        closetArea = new JTextArea();
        closetArea.setEditable(false);
        p.add(new JScrollPane(closetArea), BorderLayout.CENTER);

        JButton backBtn = new RoundButton("뒤로가기");
        backBtn.addActionListener(e -> cardLayout.show(contentPanel, "main"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(SOFT_BLUE);
        bottom.add(backBtn);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private void updateClosetPanel() {
        StringBuilder sb = new StringBuilder()
                .append("👕 상의:\n").append(getItems("상의")).append("\n\n")
                .append("👖 하의:\n").append(getItems("하의")).append("\n\n")
                .append("🎀 악세서리:\n").append(getItems("악세서리"));
        closetArea.setText(sb.toString());
    }

    private String getItems(String type) {
        return closet.stream()
                .filter(c -> c.getType().equals(type))
                .map(c -> "- " + c.getName() + " (" + c.getMinTemp() + "~" + c.getMaxTemp() + "도)")
                .collect(Collectors.joining("\n"));
    }

    private void doFetchAndRecommend() {
        fetchWeather();
        String top = null, bottom = null, acc = null;
        for (var ci : closet) {
            if (temperature >= ci.getMinTemp() && temperature <= ci.getMaxTemp()) {
                switch (ci.getType()) {
                    case "상의": if (top == null) top = ci.getName(); break;
                    case "하의": if (bottom == null) bottom = ci.getName(); break;
                    case "악세서리": if (acc == null) acc = ci.getName(); break;
                }
            }
        }
        resultArea.setText(String.format(
                "📡 날씨: %.1f도, %s\n👕 추천:\n- 상의: %s\n- 하의: %s\n- 악세서리: %s",
                temperature, condition,
                top != null ? top : "없음",
                bottom != null ? bottom : "없음",
                acc != null ? acc : "없음"
        ));
    }

    private void fetchWeather() {
        try {
            String date = dateField.getText().trim();
            LocalDate sel = LocalDate.parse(date, DATE_FMT);
            LocalDate min = LocalDate.now().minusDays(2), max = LocalDate.now().plusDays(7);
            if (sel.isBefore(min) || sel.isAfter(max)) throw new RuntimeException("조회 가능 기간: " + min.format(DATE_FMT) + "~" + max.format(DATE_FMT));

            String nx = "60", ny = "127";
            switch ((String) regionCombo.getSelectedItem()) {
                case "부산": nx = "98"; ny = "76"; break;
                case "대구": nx = "89"; ny = "90"; break;
                case "광주": nx = "58"; ny = "74"; break;
                case "대전": nx = "67"; ny = "100"; break;
            }

            boolean today = sel.equals(LocalDate.now());
            String bt = today ? LocalTime.now().format(TIME_FMT) : "0500";
            String url = (today ? ULTRA_URL : VILLAGE_URL)
                    + "?serviceKey=" + URLEncoder.encode(SERVICE_KEY, StandardCharsets.UTF_8)
                    + "&numOfRows=" + (today ? 20 : 100)
                    + "&pageNo=1&dataType=JSON"
                    + "&base_date=" + date
                    + "&base_time=" + bt
                    + "&nx=" + nx + "&ny=" + ny;

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) throw new RuntimeException("HTTP " + conn.getResponseCode());

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder raw = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) raw.append(line);
            rd.close(); conn.disconnect();

            JSONArray items = new JSONObject(raw.toString())
                    .getJSONObject("response").getJSONObject("body")
                    .getJSONObject("items").getJSONArray("item");

            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i);
                String cat = it.getString("category");
                String val = it.getString(today ? "obsrValue" : "fcstValue");
                if (cat.equals("T1H") || cat.equals("TMP")) temperature = Double.parseDouble(val);
                if (cat.equals("PTY") || cat.equals("SKY")) {
                    int code = Integer.parseInt(val);
                    condition = today ? (code == 0 ? "맑음" : "비/눈") : (code == 1 ? "맑음" : code == 3 ? "구름많음" : code == 4 ? "흐림" : "알수없음");
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private void saveClothes() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(closet);
        } catch (IOException ignored) {}
    }

    @SuppressWarnings("unchecked")
    private ArrayList<ClothingItem> loadClothes() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            return (ArrayList<ClothingItem>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherClothesApp::new);
    }
}

// ✅ 둥근 흰색 버튼 클래스
class RoundButton extends JButton {
    public RoundButton(String text) {
        super(text);
        setBackground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(getBackground());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(220, 220, 220));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        g2.dispose();
    }
}

// ✅ 옷 정보 클래스
class ClothingItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name, type;
    private final int minTemp, maxTemp;
    public ClothingItem(String name, String type, int minTemp, int maxTemp) {
        this.name = name; this.type = type; this.minTemp = minTemp; this.maxTemp = maxTemp;
    }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getMinTemp() { return minTemp; }
    public int getMaxTemp() { return maxTemp; }
}
