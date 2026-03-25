import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class JetGraderApp extends JFrame {

    private static final String SCREEN_HOME = "HOME";
    private static final String SCREEN_GRADE = "GRADE";
    private static final String SCREEN_EDIT = "EDIT";
    private static final Path ANSWERS_FILE = Paths.get("answers.json");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);

    private final Map<String, LevelConfig> levels = new LinkedHashMap<>();

    private JComboBox<String> gradeLevelCombo;
    private JPanel gradeLcContainer;
    private JPanel gradeRcContainer;
    private JTextArea gradeResultArea;
    private JLabel gradeInfoLabel;

    private final Map<String, List<DigitField>> gradeLcFields = new LinkedHashMap<>();
    private final Map<String, List<DigitField>> gradeRcFields = new LinkedHashMap<>();

    private JComboBox<String> editLevelCombo;
    private JPanel editLcContainer;
    private JPanel editRcContainer;
    private JLabel editInfoLabel;

    private final Map<String, List<DigitField>> editLcFields = new LinkedHashMap<>();
    private final Map<String, List<DigitField>> editRcFields = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new JetGraderApp().setVisible(true);
        });
    }

    public JetGraderApp() {
        initializeData();
        initializeAnswerStorage();
        initializeFrame();
        initializeScreens();
        showScreen(SCREEN_HOME);
    }

    private void initializeAnswerStorage() {
        if (Files.exists(ANSWERS_FILE)) {
            try {
                loadAnswersFromJson();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "answers.json 파일을 불러오지 못했습니다.\n기본 정답으로 시작합니다.\n\n사유: " + e.getMessage(),
                        "정답 파일 불러오기 실패",
                        JOptionPane.WARNING_MESSAGE
                );
                saveAnswersToJsonWithAlert(false);
            }
        } else {
            saveAnswersToJsonWithAlert(false);
        }
    }

    private void initializeFrame() {
        setTitle("JET 채점툴");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 800);
        setLocationRelativeTo(null);
        setContentPane(rootPanel);
    }

    private void initializeScreens() {
        rootPanel.add(buildHomeScreen(), SCREEN_HOME);
        rootPanel.add(buildGradeScreen(), SCREEN_GRADE);
        rootPanel.add(buildEditScreen(), SCREEN_EDIT);
    }

    private JPanel buildHomeScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 246, 248));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 12, 12, 12);

        JLabel title = new JLabel("JET 채점툴", SwingConstants.CENTER);
        title.setOpaque(true);
        title.setBackground(new Color(69, 114, 196));
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 36));
        title.setBorder(new CompoundBorder(
                new LineBorder(new Color(48, 78, 140), 1, true),
                new EmptyBorder(32, 56, 32, 56)
        ));
        title.setPreferredSize(new Dimension(320, 120));

        JButton gradeButton = createMainButton("채점");
        gradeButton.addActionListener(e -> {
            rebuildGradeScreen();
            showScreen(SCREEN_GRADE);
        });

        JButton editButton = createMainButton("답안수정");
        editButton.addActionListener(e -> {
            rebuildEditScreen();
            showScreen(SCREEN_EDIT);
        });

        gbc.gridy = 0;
        panel.add(title, gbc);
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(10), gbc);
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(gradeButton, gbc);
        gbc.gridy = 3;
        panel.add(editButton, gbc);

        return panel;
    }

    private JButton createMainButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Malgun Gothic", Font.PLAIN, 22));
        button.setPreferredSize(new Dimension(180, 54));
        button.setFocusPainted(false);
        return button;
    }

    private JPanel buildGradeScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setBackground(new Color(245, 246, 248));

        panel.add(buildGradeTopBar(), BorderLayout.NORTH);
        panel.add(buildGradeCenter(), BorderLayout.CENTER);
        panel.add(buildGradeBottom(), BorderLayout.SOUTH);

        rebuildGradeScreen();
        return panel;
    }

    private JPanel buildGradeTopBar() {
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel label = new JLabel("레벨:");
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 16));

        gradeLevelCombo = new JComboBox<>(levels.keySet().toArray(new String[0]));
        gradeLevelCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
        gradeLevelCombo.setPreferredSize(new Dimension(160, 34));
        gradeLevelCombo.addActionListener(e -> rebuildGradeScreen());

        gradeInfoLabel = new JLabel();
        gradeInfoLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));

        left.add(label);
        left.add(gradeLevelCombo);
        left.add(gradeInfoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton homeButton = new JButton("메인으로");
        homeButton.addActionListener(e -> showScreen(SCREEN_HOME));

        right.add(homeButton);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel buildGradeCenter() {
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
        center.setOpaque(false);

        gradeLcContainer = createCardSection("LC 입력");
        gradeRcContainer = createCardSection("RC 입력");

        center.add(new JScrollPane(gradeLcContainer));
        center.add(new JScrollPane(gradeRcContainer));
        return center;
    }

    private JPanel buildGradeBottom() {
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton clearButton = new JButton("입력 초기화");
        clearButton.addActionListener(e -> clearGradeInputs());

        JButton gradeButton = new JButton("채점하기");
        gradeButton.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        gradeButton.addActionListener(e -> performGrading());

        buttons.add(clearButton);
        buttons.add(gradeButton);

        gradeResultArea = new JTextArea(13, 20);
        gradeResultArea.setEditable(false);
        gradeResultArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        gradeResultArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(gradeResultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("채점 결과"));

        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(scrollPane, BorderLayout.CENTER);
        return bottom;
    }

    private JPanel buildEditScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setBackground(new Color(245, 246, 248));

        panel.add(buildEditTopBar(), BorderLayout.NORTH);
        panel.add(buildEditCenter(), BorderLayout.CENTER);
        panel.add(buildEditBottom(), BorderLayout.SOUTH);

        rebuildEditScreen();
        return panel;
    }

    private JPanel buildEditTopBar() {
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel label = new JLabel("답안 수정 레벨:");
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 16));

        editLevelCombo = new JComboBox<>(levels.keySet().toArray(new String[0]));
        editLevelCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
        editLevelCombo.setPreferredSize(new Dimension(160, 34));
        editLevelCombo.addActionListener(e -> rebuildEditScreen());

        editInfoLabel = new JLabel();
        editInfoLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));

        left.add(label);
        left.add(editLevelCombo);
        left.add(editInfoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton homeButton = new JButton("메인으로");
        homeButton.addActionListener(e -> showScreen(SCREEN_HOME));
        right.add(homeButton);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel buildEditCenter() {
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
        center.setOpaque(false);

        editLcContainer = createCardSection("LC 정답 수정");
        editRcContainer = createCardSection("RC 정답 수정");

        center.add(new JScrollPane(editLcContainer));
        center.add(new JScrollPane(editRcContainer));
        return center;
    }

    private JPanel buildEditBottom() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton reloadButton = new JButton("파일 다시 불러오기");
        reloadButton.addActionListener(e -> reloadAnswersFromDisk());

        JButton resetButton = new JButton("현재 메모리값 다시 불러오기");
        resetButton.addActionListener(e -> rebuildEditScreen());

        JButton saveButton = new JButton("저장하기");
        saveButton.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        saveButton.addActionListener(e -> saveEditedAnswers());

        bottom.add(reloadButton);
        bottom.add(resetButton);
        bottom.add(saveButton);
        return bottom;
    }

    private JPanel createCardSection(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.setBackground(Color.WHITE);
        return section;
    }

    private void rebuildGradeScreen() {
        if (gradeLevelCombo == null) {
            return;
        }

        String levelName = (String) gradeLevelCombo.getSelectedItem();
        LevelConfig level = levels.get(levelName);

        gradeLcFields.clear();
        gradeRcFields.clear();
        gradeLcContainer.removeAll();
        gradeRcContainer.removeAll();
        gradeResultArea.setText("");

        gradeInfoLabel.setText("입력 후 한 칸씩 자동 이동 | 정답 파일: " + ANSWERS_FILE.toAbsolutePath());

        buildAnswerInputRows(gradeLcContainer, gradeLcFields, level.lcParts);
        buildAnswerInputRows(gradeRcContainer, gradeRcFields, level.rcParts);

        gradeLcContainer.revalidate();
        gradeLcContainer.repaint();
        gradeRcContainer.revalidate();
        gradeRcContainer.repaint();

        focusFirstGradeField();
    }

    private void rebuildEditScreen() {
        if (editLevelCombo == null) {
            return;
        }

        String levelName = (String) editLevelCombo.getSelectedItem();
        LevelConfig level = levels.get(levelName);

        editLcFields.clear();
        editRcFields.clear();
        editLcContainer.removeAll();
        editRcContainer.removeAll();

        editInfoLabel.setText("저장 시 answers.json 파일까지 함께 갱신됨");

        buildAnswerEditRows(editLcContainer, editLcFields, level.lcParts, level.lcAnswers);
        buildAnswerEditRows(editRcContainer, editRcFields, level.rcParts, level.rcAnswers);

        editLcContainer.revalidate();
        editLcContainer.repaint();
        editRcContainer.revalidate();
        editRcContainer.repaint();
    }

    private void buildAnswerInputRows(JPanel container, Map<String, List<DigitField>> fieldMap, List<PartConfig> parts) {
        for (PartConfig part : parts) {
            JPanel partPanel = new JPanel(new BorderLayout(8, 8));
            partPanel.setOpaque(false);
            partPanel.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, new Color(225, 225, 225)),
                    new EmptyBorder(12, 12, 12, 12)
            ));

            JLabel partLabel = new JLabel(part.name + "  (" + part.count + "문항 / 배점 " + formatNumber(part.weight) + ")");
            partLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 15));

            JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            fieldsPanel.setOpaque(false);
            List<DigitField> fields = createDigitFieldList(part.count);
            wireAutoMove(fields);

            for (int i = 0; i < fields.size(); i++) {
                JPanel wrap = new JPanel(new BorderLayout());
                wrap.setOpaque(false);

                JLabel num = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
                num.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
                num.setForeground(new Color(100, 100, 100));

                wrap.add(fields.get(i), BorderLayout.CENTER);
                wrap.add(num, BorderLayout.SOUTH);
                fieldsPanel.add(wrap);
            }

            partPanel.add(partLabel, BorderLayout.NORTH);
            partPanel.add(fieldsPanel, BorderLayout.CENTER);

            container.add(partPanel);
            fieldMap.put(part.name, fields);
        }
        container.add(Box.createVerticalGlue());
    }

    private void buildAnswerEditRows(JPanel container,
                                     Map<String, List<DigitField>> fieldMap,
                                     List<PartConfig> parts,
                                     Map<String, List<String>> answers) {
        for (PartConfig part : parts) {
            JPanel partPanel = new JPanel(new BorderLayout(8, 8));
            partPanel.setOpaque(false);
            partPanel.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, new Color(225, 225, 225)),
                    new EmptyBorder(12, 12, 12, 12)
            ));

            JLabel partLabel = new JLabel(part.name + "  (" + part.count + "문항)");
            partLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 15));

            JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            fieldsPanel.setOpaque(false);
            List<DigitField> fields = createDigitFieldList(part.count);
            wireAutoMove(fields);

            List<String> savedAnswers = answers.get(part.name);
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setText(savedAnswers.get(i));

                JPanel wrap = new JPanel(new BorderLayout());
                wrap.setOpaque(false);

                JLabel num = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
                num.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
                num.setForeground(new Color(100, 100, 100));

                wrap.add(fields.get(i), BorderLayout.CENTER);
                wrap.add(num, BorderLayout.SOUTH);
                fieldsPanel.add(wrap);
            }

            partPanel.add(partLabel, BorderLayout.NORTH);
            partPanel.add(fieldsPanel, BorderLayout.CENTER);

            container.add(partPanel);
            fieldMap.put(part.name, fields);
        }
        container.add(Box.createVerticalGlue());
    }

    private List<DigitField> createDigitFieldList(int count) {
        List<DigitField> fields = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            fields.add(new DigitField());
        }
        return fields;
    }

    private void wireAutoMove(List<DigitField> fields) {
        for (int i = 0; i < fields.size(); i++) {
            DigitField current = fields.get(i);
            DigitField prev = i > 0 ? fields.get(i - 1) : null;
            DigitField next = i < fields.size() - 1 ? fields.get(i + 1) : null;
            current.setNavigationTargets(prev, next);
        }
    }

    private void focusFirstGradeField() {
        SwingUtilities.invokeLater(() -> {
            for (List<DigitField> fields : gradeLcFields.values()) {
                if (!fields.isEmpty()) {
                    fields.get(0).requestFocusInWindow();
                    break;
                }
            }
        });
    }

    private void clearGradeInputs() {
        clearFieldMap(gradeLcFields);
        clearFieldMap(gradeRcFields);
        gradeResultArea.setText("");
        focusFirstGradeField();
    }

    private void clearFieldMap(Map<String, List<DigitField>> fieldMap) {
        for (List<DigitField> list : fieldMap.values()) {
            for (DigitField field : list) {
                field.setText("");
            }
        }
    }

    private void performGrading() {
        String levelName = (String) gradeLevelCombo.getSelectedItem();
        LevelConfig level = levels.get(levelName);

        GroupScore lcScore = scoreGroup(level.lcParts, level.lcAnswers, gradeLcFields, "LC");
        if (lcScore == null) return;

        GroupScore rcScore = scoreGroup(level.rcParts, level.rcAnswers, gradeRcFields, "RC");
        if (rcScore == null) return;

        double lc = level.baseLC + lcScore.score;
        double rc = level.baseRC + rcScore.score;
        double total = lc + rc;

        double lcMax = level.baseLC + calculateMax(level.lcParts);
        double rcMax = level.baseRC + calculateMax(level.rcParts);
        double totalMax = lcMax + rcMax;

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(levelName).append(" 채점 결과]\n\n");
        sb.append("LC: ").append(formatNumber(lc)).append(" / ").append(formatNumber(lcMax)).append("\n");
        sb.append("RC: ").append(formatNumber(rc)).append(" / ").append(formatNumber(rcMax)).append("\n");
        sb.append("총점: ").append(formatNumber(total)).append(" / ").append(formatNumber(totalMax)).append("\n\n");

        sb.append("LC 세부\n");
        for (PartSummary s : lcScore.summaries) {
            sb.append("- ").append(s.partName).append(": ")
                    .append(s.correctCount).append("/").append(s.totalCount).append("\n");
        }

        sb.append("\nRC 세부\n");
        for (PartSummary s : rcScore.summaries) {
            sb.append("- ").append(s.partName).append(": ")
                    .append(s.correctCount).append("/").append(s.totalCount).append("\n");
        }

        gradeResultArea.setText(sb.toString());
    }

    private GroupScore scoreGroup(List<PartConfig> parts,
                                  Map<String, List<String>> answerMap,
                                  Map<String, List<DigitField>> inputMap,
                                  String groupLabel) {
        double score = 0.0;
        List<PartSummary> summaries = new ArrayList<>();

        for (PartConfig part : parts) {
            List<DigitField> fields = inputMap.get(part.name);
            List<String> correct = answerMap.get(part.name);

            int got = 0;
            for (int i = 0; i < part.count; i++) {
                String value = fields.get(i).getText().trim();
                if (!value.matches("[1-5]")) {
                    JOptionPane.showMessageDialog(
                            this,
                            groupLabel + " " + part.name + "의 " + (i + 1) + "번 칸을 확인하세요.\n1~5 중 하나만 입력해야 합니다.",
                            "입력 오류",
                            JOptionPane.ERROR_MESSAGE
                    );
                    fields.get(i).requestFocusInWindow();
                    return null;
                }
                if (value.equals(correct.get(i))) {
                    got++;
                }
            }

            score += got * part.weight;
            summaries.add(new PartSummary(part.name, got, part.count));
        }

        return new GroupScore(score, summaries);
    }

    private void saveEditedAnswers() {
        String levelName = (String) editLevelCombo.getSelectedItem();
        LevelConfig level = levels.get(levelName);

        if (!applyEditedGroup(level.lcParts, level.lcAnswers, editLcFields, "LC")) {
            return;
        }
        if (!applyEditedGroup(level.rcParts, level.rcAnswers, editRcFields, "RC")) {
            return;
        }

        if (!saveAnswersToJsonWithAlert(true)) {
            return;
        }

        JOptionPane.showMessageDialog(this,
                levelName + " 답안이 저장되었습니다.\nanswers.json 파일도 함께 갱신되었습니다.\n메인 화면으로 돌아갑니다.",
                "저장 완료",
                JOptionPane.INFORMATION_MESSAGE);

        rebuildGradeScreen();
        showScreen(SCREEN_HOME);
    }

    private boolean applyEditedGroup(List<PartConfig> parts,
                                     Map<String, List<String>> targetAnswers,
                                     Map<String, List<DigitField>> editMap,
                                     String groupLabel) {
        for (PartConfig part : parts) {
            List<DigitField> fields = editMap.get(part.name);
            List<String> newAnswers = new ArrayList<>();

            for (int i = 0; i < part.count; i++) {
                String value = fields.get(i).getText().trim();
                if (!value.matches("[1-5]")) {
                    JOptionPane.showMessageDialog(
                            this,
                            groupLabel + " " + part.name + "의 " + (i + 1) + "번 정답을 확인하세요.\n1~5 중 하나만 입력해야 합니다.",
                            "입력 오류",
                            JOptionPane.ERROR_MESSAGE
                    );
                    fields.get(i).requestFocusInWindow();
                    return false;
                }
                newAnswers.add(value);
            }

            targetAnswers.put(part.name, newAnswers);
        }
        return true;
    }

    private void reloadAnswersFromDisk() {
        try {
            if (!Files.exists(ANSWERS_FILE)) {
                JOptionPane.showMessageDialog(this,
                        "answers.json 파일이 아직 없습니다.\n기본값을 먼저 저장합니다.",
                        "파일 없음",
                        JOptionPane.WARNING_MESSAGE);
                saveAnswersToJsonWithAlert(true);
            } else {
                loadAnswersFromJson();
                rebuildEditScreen();
                rebuildGradeScreen();
                JOptionPane.showMessageDialog(this,
                        "answers.json 파일을 다시 불러왔습니다.",
                        "불러오기 완료",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "answers.json 파일을 다시 불러오지 못했습니다.\n사유: " + e.getMessage(),
                    "불러오기 실패",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean saveAnswersToJsonWithAlert(boolean showError) {
        try {
            Files.writeString(
                    ANSWERS_FILE,
                    buildAnswersJson(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return true;
        } catch (IOException e) {
            if (showError) {
                JOptionPane.showMessageDialog(this,
                        "answers.json 저장에 실패했습니다.\n사유: " + e.getMessage(),
                        "저장 실패",
                        JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    private void loadAnswersFromJson() throws IOException {
        String json = Files.readString(ANSWERS_FILE, StandardCharsets.UTF_8);
        Object parsed = new SimpleJsonParser(json).parse();
        if (!(parsed instanceof Map)) {
            throw new IOException("JSON 최상위 구조가 객체가 아닙니다.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parsed;
        Object levelsNode = root.get("levels");
        if (!(levelsNode instanceof Map)) {
            throw new IOException("levels 항목이 없습니다.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> levelMap = (Map<String, Object>) levelsNode;

        for (Map.Entry<String, LevelConfig> levelEntry : levels.entrySet()) {
            String levelName = levelEntry.getKey();
            LevelConfig level = levelEntry.getValue();
            Object levelNode = levelMap.get(levelName);
            if (!(levelNode instanceof Map)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> levelObject = (Map<String, Object>) levelNode;
            applyJsonGroup(level.lcParts, level.lcAnswers, levelObject.get("LC"));
            applyJsonGroup(level.rcParts, level.rcAnswers, levelObject.get("RC"));
        }
    }

    private void applyJsonGroup(List<PartConfig> parts,
                                Map<String, List<String>> targetMap,
                                Object groupNode) throws IOException {
        if (!(groupNode instanceof Map)) {
            throw new IOException("LC/RC 구조가 올바르지 않습니다.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> groupMap = (Map<String, Object>) groupNode;

        for (PartConfig part : parts) {
            Object partNode = groupMap.get(part.name);
            if (!(partNode instanceof List)) {
                throw new IOException(part.name + " 배열이 없습니다.");
            }

            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) partNode;
            if (rawList.size() != part.count) {
                throw new IOException(part.name + " 문항 수가 맞지 않습니다. 필요=" + part.count + ", 실제=" + rawList.size());
            }

            List<String> newAnswers = new ArrayList<>();
            for (Object obj : rawList) {
                String value = String.valueOf(obj).trim();
                if (!value.matches("[1-5]")) {
                    throw new IOException(part.name + " 정답 값이 1~5가 아닙니다: " + value);
                }
                newAnswers.add(value);
            }
            targetMap.put(part.name, newAnswers);
        }
    }

    private String buildAnswersJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"levels\": {\n");

        int levelIndex = 0;
        for (LevelConfig level : levels.values()) {
            if (levelIndex++ > 0) {
                sb.append(",\n");
            }
            sb.append("    \"").append(escapeJson(level.name)).append("\": {\n");
            sb.append("      \"LC\": ").append(groupToJson(level.lcParts, level.lcAnswers, 6)).append(",\n");
            sb.append("      \"RC\": ").append(groupToJson(level.rcParts, level.rcAnswers, 6)).append("\n");
            sb.append("    }");
        }

        sb.append("\n  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String groupToJson(List<PartConfig> parts, Map<String, List<String>> answerMap, int indentSpaces) {
        String indent = " ".repeat(indentSpaces);
        String innerIndent = " ".repeat(indentSpaces + 2);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        for (int i = 0; i < parts.size(); i++) {
            PartConfig part = parts.get(i);
            List<String> answers = answerMap.get(part.name);
            sb.append(innerIndent)
                    .append("\"")
                    .append(escapeJson(part.name))
                    .append("\": ")
                    .append(stringListToJson(answers));
            if (i < parts.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(indent).append("}");
        return sb.toString();
    }

    private String stringListToJson(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(values.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private double calculateMax(List<PartConfig> parts) {
        double sum = 0.0;
        for (PartConfig part : parts) {
            sum += part.count * part.weight;
        }
        return sum;
    }

    private void showScreen(String name) {
        cardLayout.show(rootPanel, name);
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 1e-9) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void initializeData() {
        LevelConfig beginner = new LevelConfig("초급", 4.0, 4.0);
        beginner.lcParts = Arrays.asList(
                new PartConfig("파트1", 6, 3.0),
                new PartConfig("파트2", 12, 3.0),
                new PartConfig("파트3", 10, 3.0),
                new PartConfig("파트4", 8, 4.0),
                new PartConfig("파트5", 4, 4.0)
        );
        beginner.rcParts = Arrays.asList(
                new PartConfig("파트6", 5, 4.0),
                new PartConfig("파트7", 5, 4.0)
        );
        beginner.lcAnswers.put("파트1", new ArrayList<>(Arrays.asList("2", "3", "3", "2", "1", "1")));
        beginner.lcAnswers.put("파트2", new ArrayList<>(Arrays.asList("1", "3", "2", "2", "1", "2", "1", "2", "1", "1", "3", "3")));
        beginner.lcAnswers.put("파트3", new ArrayList<>(Arrays.asList("1", "2", "3", "1", "3", "1", "2", "3", "1", "2")));
        beginner.lcAnswers.put("파트4", new ArrayList<>(Arrays.asList("3", "2", "3", "3", "2", "2", "3", "3")));
        beginner.lcAnswers.put("파트5", new ArrayList<>(Arrays.asList("3", "1", "3", "3")));
        beginner.rcAnswers.put("파트6", new ArrayList<>(Arrays.asList("2", "2", "3", "3", "1")));
        beginner.rcAnswers.put("파트7", new ArrayList<>(Arrays.asList("1", "2", "3", "1", "3")));
        levels.put(beginner.name, beginner);

        LevelConfig intermediate = new LevelConfig("중급", 0.0, 0.0);
        intermediate.lcParts = Arrays.asList(
                new PartConfig("파트1", 6, 2.0),
                new PartConfig("파트2", 10, 2.0),
                new PartConfig("파트3", 8, 3.0),
                new PartConfig("파트4", 5, 3.0),
                new PartConfig("파트5", 6, 3.5)
        );
        intermediate.rcParts = Arrays.asList(
                new PartConfig("파트6", 6, 4.0),
                new PartConfig("파트7", 6, 4.0),
                new PartConfig("파트8", 8, 5.0)
        );
        intermediate.lcAnswers.put("파트1", new ArrayList<>(Arrays.asList("3", "3", "1", "1", "2", "2")));
        intermediate.lcAnswers.put("파트2", new ArrayList<>(Arrays.asList("1", "1", "2", "3", "3", "1", "1", "2", "3", "2")));
        intermediate.lcAnswers.put("파트3", new ArrayList<>(Arrays.asList("2", "1", "2", "1", "1", "3", "2", "1")));
        intermediate.lcAnswers.put("파트4", new ArrayList<>(Arrays.asList("1", "2", "3", "2", "1")));
        intermediate.lcAnswers.put("파트5", new ArrayList<>(Arrays.asList("3", "1", "1", "2", "3", "1")));
        intermediate.rcAnswers.put("파트6", new ArrayList<>(Arrays.asList("2", "1", "3", "1", "3", "3")));
        intermediate.rcAnswers.put("파트7", new ArrayList<>(Arrays.asList("2", "1", "3", "2", "1", "3")));
        intermediate.rcAnswers.put("파트8", new ArrayList<>(Arrays.asList("2", "2", "1", "1", "2", "1", "3", "3")));
        levels.put(intermediate.name, intermediate);

        LevelConfig advanced = new LevelConfig("고급", 0.0, 0.0);
        advanced.lcParts = Arrays.asList(
                new PartConfig("파트1", 6, 2.0),
                new PartConfig("파트2", 10, 2.0),
                new PartConfig("파트3", 6, 3.0),
                new PartConfig("파트4", 8, 3.0),
                new PartConfig("파트5", 5, 3.5)
        );
        advanced.rcParts = Arrays.asList(
                new PartConfig("파트6", 8, 3.0),
                new PartConfig("파트7", 7, 3.5),
                new PartConfig("파트8", 10, 4.0)
        );
        advanced.lcAnswers.put("파트1", new ArrayList<>(Arrays.asList("1", "2", "1", "2", "3", "3")));
        advanced.lcAnswers.put("파트2", new ArrayList<>(Arrays.asList("1", "1", "2", "3", "1", "3", "2", "2", "1", "2")));
        advanced.lcAnswers.put("파트3", new ArrayList<>(Arrays.asList("1", "2", "3", "3", "2", "1")));
        advanced.lcAnswers.put("파트4", new ArrayList<>(Arrays.asList("1", "3", "2", "1", "2", "3", "1", "3")));
        advanced.lcAnswers.put("파트5", new ArrayList<>(Arrays.asList("1", "2", "3", "1", "1")));
        advanced.rcAnswers.put("파트6", new ArrayList<>(Arrays.asList("2", "1", "3", "3", "1", "2", "1", "3")));
        advanced.rcAnswers.put("파트7", new ArrayList<>(Arrays.asList("1", "2", "3", "1", "2", "3", "3")));
        advanced.rcAnswers.put("파트8", new ArrayList<>(Arrays.asList("2", "1", "3", "1", "1", "3", "1", "2", "2", "3")));
        levels.put(advanced.name, advanced);
    }

    static class PartConfig {
        final String name;
        final int count;
        final double weight;

        PartConfig(String name, int count, double weight) {
            this.name = name;
            this.count = count;
            this.weight = weight;
        }
    }

    static class LevelConfig {
        final String name;
        final double baseLC;
        final double baseRC;
        List<PartConfig> lcParts = new ArrayList<>();
        List<PartConfig> rcParts = new ArrayList<>();
        Map<String, List<String>> lcAnswers = new LinkedHashMap<>();
        Map<String, List<String>> rcAnswers = new LinkedHashMap<>();

        LevelConfig(String name, double baseLC, double baseRC) {
            this.name = name;
            this.baseLC = baseLC;
            this.baseRC = baseRC;
        }
    }

    static class PartSummary {
        final String partName;
        final int correctCount;
        final int totalCount;

        PartSummary(String partName, int correctCount, int totalCount) {
            this.partName = partName;
            this.correctCount = correctCount;
            this.totalCount = totalCount;
        }
    }

    static class GroupScore {
        final double score;
        final List<PartSummary> summaries;

        GroupScore(double score, List<PartSummary> summaries) {
            this.score = score;
            this.summaries = summaries;
        }
    }

    static class DigitField extends JTextField {
        private DigitField previousField;
        private DigitField nextField;

        DigitField() {
            super(1);
            setHorizontalAlignment(JTextField.CENTER);
            setFont(new Font("Consolas", Font.BOLD, 18));
            setPreferredSize(new Dimension(44, 36));
            setBorder(new CompoundBorder(new LineBorder(new Color(170, 170, 170)), new EmptyBorder(4, 4, 4, 4)));

            ((AbstractDocument) getDocument()).setDocumentFilter(new SingleDigitFilter());

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        if (getText().isEmpty() && previousField != null) {
                            previousField.requestFocusInWindow();
                            previousField.setText("");
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT && previousField != null) {
                        previousField.requestFocusInWindow();
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && nextField != null) {
                        nextField.requestFocusInWindow();
                    }
                }
            });
        }

        void setNavigationTargets(DigitField previousField, DigitField nextField) {
            this.previousField = previousField;
            this.nextField = nextField;
        }

        private class SingleDigitFilter extends DocumentFilter {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) {
                    return;
                }

                String normalized = text.trim();
                if (normalized.isEmpty()) {
                    super.replace(fb, 0, fb.getDocument().getLength(), "", attrs);
                    return;
                }

                char ch = normalized.charAt(normalized.length() - 1);
                if (ch < '1' || ch > '5') {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                super.replace(fb, 0, fb.getDocument().getLength(), String.valueOf(ch), attrs);
                SwingUtilities.invokeLater(() -> {
                    if (nextField != null) {
                        nextField.requestFocusInWindow();
                        nextField.selectAll();
                    }
                });
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
            }
        }
    }

    static class SimpleJsonParser {
        private final String text;
        private int index;

        SimpleJsonParser(String text) {
            this.text = text;
        }

        Object parse() throws IOException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw new IOException("JSON 끝 이후에 불필요한 내용이 있습니다.");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IOException("예상보다 빨리 JSON이 끝났습니다.");
            }

            char ch = text.charAt(index);
            if (ch == '{') return parseObject();
            if (ch == '[') return parseArray();
            if (ch == '"') return parseString();
            if (ch == '-' || Character.isDigit(ch)) return parseNumber();
            if (match("true")) return Boolean.TRUE;
            if (match("false")) return Boolean.FALSE;
            if (match("null")) return null;

            throw new IOException("알 수 없는 JSON 값 시작: " + ch);
        }

        private Map<String, Object> parseObject() throws IOException {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() throws IOException {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return list;
            }

            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw new IOException("문자열 이스케이프가 잘못되었습니다.");
                    }
                    char esc = text.charAt(index++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (index + 4 > text.length()) {
                                throw new IOException("유니코드 이스케이프가 잘못되었습니다.");
                            }
                            String hex = text.substring(index, index + 4);
                            index += 4;
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                throw new IOException("유니코드 이스케이프가 잘못되었습니다: " + hex);
                            }
                            break;
                        default:
                            throw new IOException("지원하지 않는 이스케이프 문자: \\" + esc);
                    }
                } else {
                    sb.append(ch);
                }
            }
            throw new IOException("문자열이 닫히지 않았습니다.");
        }

        private Object parseNumber() throws IOException {
            int start = index;
            if (text.charAt(index) == '-') index++;
            while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
            if (index < text.length() && text.charAt(index) == '.') {
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
            }
            String number = text.substring(start, index);
            try {
                if (number.contains(".")) {
                    return Double.parseDouble(number);
                }
                return Long.parseLong(number);
            } catch (NumberFormatException e) {
                throw new IOException("숫자 형식이 잘못되었습니다: " + number);
            }
        }

        private boolean match(String keyword) {
            if (text.startsWith(keyword, index)) {
                index += keyword.length();
                return true;
            }
            return false;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void expect(char expected) throws IOException {
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IOException("'" + expected + "' 문자가 필요합니다.");
            }
            index++;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
