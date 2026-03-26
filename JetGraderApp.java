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

    private static final Color BG = new Color(243, 246, 251);
    private static final Color CARD = new Color(255, 255, 255);
    private static final Color CARD_SOFT = new Color(248, 250, 253);
    private static final Color PRIMARY = new Color(59, 91, 219);
    private static final Color PRIMARY_DARK = new Color(43, 69, 176);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color SUCCESS = new Color(13, 148, 136);
    private static final Color ORANGE = new Color(217, 119, 6);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color BTN_BLUE = new Color(52, 120, 246);
    private static final Color BTN_GREEN = new Color(34, 197, 94);
    private static final Color BTN_PURPLE = new Color(139, 92, 246);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);

    private final MonthlyExamConfig monthlyConfig = new MonthlyExamConfig();
    private final MockExamConfig mockConfig = new MockExamConfig();
    private final AchievementExamConfig achievementConfig = new AchievementExamConfig();

    private ToolType currentGradeTool = ToolType.MONTHLY;

    private JPanel gradeHeaderMetaPanel;
    private JLabel gradeToolLabel;
    private JComboBox<String> gradeLevelCombo;
    private JPanel gradeDynamicContainer;
    private JTextArea gradeResultArea;
    private JLabel gradePathLabel;
    private JLabel gradeSummaryLabel;

    private final Map<String, List<DigitField>> monthlyGradeLcFields = new LinkedHashMap<>();
    private final Map<String, List<DigitField>> monthlyGradeRcFields = new LinkedHashMap<>();

    private final List<DigitField> mockGradeFields = new ArrayList<>();

    private final List<DigitField> achLcMcFields = new ArrayList<>();
    private final List<OXField> achLcSaFields = new ArrayList<>();
    private final List<DigitField> achRcMcFields = new ArrayList<>();
    private final List<OXField> achRcSaFields = new ArrayList<>();

    private JComboBox<String> editToolCombo;
    private JComboBox<String> editLevelCombo;
    private JPanel editDynamicContainer;
    private JLabel editPathLabel;
    private JLabel editInfoLabel;

    private final Map<String, List<DigitField>> monthlyEditLcFields = new LinkedHashMap<>();
    private final Map<String, List<DigitField>> monthlyEditRcFields = new LinkedHashMap<>();

    private final List<DigitField> mockEditAnswerFields = new ArrayList<>();
    private JTextField mockThreePointField;

    private final List<DigitField> achEditLcFields = new ArrayList<>();
    private final List<DigitField> achEditRcFields = new ArrayList<>();

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

    private void initializeData() {
        monthlyConfig.initializeDefaults();
        mockConfig.initializeDefaults();
        achievementConfig.initializeDefaults();
    }

    private void initializeAnswerStorage() {
        if (Files.exists(ANSWERS_FILE)) {
            try {
                loadAnswersFromJson();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "answers.json 파일을 불러오지 못했습니다.\n기본 정답으로 시작합니다.\n\n사유: " + e.getMessage(),
                        "정답 파일 불러오기 실패",
                        JOptionPane.WARNING_MESSAGE);
                saveAnswersToJsonWithAlert(false);
            }
        } else {
            saveAnswersToJsonWithAlert(false);
        }
    }

    private void initializeFrame() {
        setTitle("통합 채점툴");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 900);
        setMinimumSize(new Dimension(1220, 780));
        setLocationRelativeTo(null);
        setContentPane(rootPanel);
    }

    private void initializeScreens() {
        rootPanel.setBackground(BG);
        rootPanel.add(buildHomeScreen(), SCREEN_HOME);
        rootPanel.add(buildGradeScreen(), SCREEN_GRADE);
        rootPanel.add(buildEditScreen(), SCREEN_EDIT);
    }

    private JPanel buildHomeScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel hero = createRoundedPanel(CARD, new EmptyBorder(30, 32, 30, 32));
        hero.setLayout(new BorderLayout(22, 22));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heroLeft = new JPanel();
        heroLeft.setOpaque(false);
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));
        heroLeft.add(createBadge("GKD INTEGRATED GRADER"));
        heroLeft.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("통합 채점툴");
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 34));
        title.setForeground(TEXT);
        heroLeft.add(title);
        heroLeft.add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("JET, 모의고사, 학업성취도 평가를 하나의 프로그램에서 관리");
        subtitle.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
        subtitle.setForeground(MUTED);
        heroLeft.add(subtitle);
        heroLeft.add(Box.createVerticalStrut(24));

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metrics.setOpaque(false);
        metrics.add(createTinyInfoCard("채점 모드", "3종 통합 운영"));
        metrics.add(createTinyInfoCard("답안 저장", "answers.json 유지"));
        metrics.add(createTinyInfoCard("입력 방식", "자동 이동 지원"));
        heroLeft.add(metrics);

        JPanel heroRight = createRoundedPanel(new Color(238, 243, 255), new EmptyBorder(22, 22, 22, 22));
        heroRight.setLayout(new BoxLayout(heroRight, BoxLayout.Y_AXIS));
        heroRight.setPreferredSize(new Dimension(330, 210));

        JLabel fileTitle = new JLabel("현재 정답 파일 위치");
        fileTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        fileTitle.setForeground(TEXT);
        heroRight.add(fileTitle);
        heroRight.add(Box.createVerticalStrut(12));

        JLabel filePath = new JLabel(toMultilineHtml(ANSWERS_FILE.toAbsolutePath().toString(), 255));
        filePath.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        filePath.setForeground(MUTED);
        heroRight.add(filePath);
        heroRight.add(Box.createVerticalGlue());

        JLabel tip = new JLabel("답안 수정 후 저장하면 세 가지 채점기가 모두 재실행 후 유지됩니다.");
        tip.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        tip.setForeground(PRIMARY_DARK);
        heroRight.add(tip);

        hero.add(heroLeft, BorderLayout.CENTER);
        hero.add(heroRight, BorderLayout.EAST);
        content.add(hero);
        content.add(Box.createVerticalStrut(22));

        JLabel sectionTitle = new JLabel("채점 도구 선택");
        sectionTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        sectionTitle.setForeground(TEXT);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sectionTitle);
        content.add(Box.createVerticalStrut(14));

        JPanel actions = new JPanel(new GridLayout(1, 3, 18, 18));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(createHomeActionCard("JET 채점", "초급 · 중급 · 고급 레벨별 LC / RC 배점 채점", "레벨별 파트 구조", PRIMARY, e -> openGradeTool(ToolType.MONTHLY)));
        actions.add(createHomeActionCard("모의고사 채점", "45문항 모의고사 채점과 3점 문항, 오답 번호 확인", "LC 1~17 / RC 18~45", ORANGE, e -> openGradeTool(ToolType.MOCK)));
        actions.add(createHomeActionCard("학업성취도 평가 채점", "LC→RC 순서의 객관식 / 서답형 o/x 채점", "객관식 + 서답형 분리", RED, e -> openGradeTool(ToolType.ACHIEVEMENT)));
        content.add(actions);
        content.add(Box.createVerticalStrut(18));

        JPanel answerCard = createRoundedPanel(CARD_SOFT, new EmptyBorder(18, 20, 18, 20));
        answerCard.setLayout(new BorderLayout(16, 8));
        answerCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel answerLeft = new JPanel();
        answerLeft.setOpaque(false);
        answerLeft.setLayout(new BoxLayout(answerLeft, BoxLayout.Y_AXIS));
        JLabel aTitle = new JLabel("답안 수정");
        aTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        aTitle.setForeground(TEXT);
        JLabel aDesc = new JLabel("들어가면 세 가지 채점기 중 원하는 항목을 선택해서 정답을 수정할 수 있습니다.");
        aDesc.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        aDesc.setForeground(MUTED);
        answerLeft.add(aTitle);
        answerLeft.add(Box.createVerticalStrut(8));
        answerLeft.add(aDesc);

        JButton editButton = createPrimaryButton("답안 수정 열기", BTN_PURPLE);
        editButton.addActionListener(e -> {
            rebuildEditScreen();
            showScreen(SCREEN_EDIT);
        });

        answerCard.add(answerLeft, BorderLayout.CENTER);
        answerCard.add(editButton, BorderLayout.EAST);
        content.add(answerCard);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private void openGradeTool(ToolType type) {
        currentGradeTool = type;
        rebuildGradeScreen();
        showScreen(SCREEN_GRADE);
    }

    private JPanel buildGradeScreen() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        panel.setBackground(BG);
        panel.add(buildGradeTopBar(), BorderLayout.NORTH);
        panel.add(buildGradeCenter(), BorderLayout.CENTER);
        panel.add(buildGradeBottom(), BorderLayout.SOUTH);
        rebuildGradeScreen();
        return panel;
    }

    private JPanel buildGradeTopBar() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel top = createRoundedPanel(CARD, new EmptyBorder(18, 20, 18, 20));
        top.setLayout(new BorderLayout(16, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        gradeToolLabel = new JLabel();
        gradeToolLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        gradeToolLabel.setForeground(TEXT);

        gradeLevelCombo = new JComboBox<>(new String[]{"초급", "중급", "고급"});
        styleComboBox(gradeLevelCombo);
        gradeLevelCombo.addActionListener(e -> {
            if (currentGradeTool == ToolType.MONTHLY) {
                rebuildGradeScreen();
            }
        });

        left.add(gradeToolLabel);
        left.add(gradeLevelCombo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton homeButton = createSecondaryButton("메인으로");
        homeButton.addActionListener(e -> showScreen(SCREEN_HOME));
        right.add(homeButton);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JPanel sub = createRoundedPanel(CARD_SOFT, new EmptyBorder(14, 18, 14, 18));
        sub.setLayout(new GridLayout(1, 2, 14, 14));
        gradePathLabel = createMetaLabel();
        gradeSummaryLabel = createMetaLabel();
        sub.add(gradePathLabel);
        sub.add(gradeSummaryLabel);

        wrapper.add(top);
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(sub);
        return wrapper;
    }

    private JPanel buildGradeCenter() {
        gradeDynamicContainer = new JPanel(new BorderLayout());
        gradeDynamicContainer.setOpaque(false);
        return gradeDynamicContainer;
    }

    private JPanel buildGradeBottom() {
        JPanel bottom = new JPanel(new BorderLayout(12, 12));
        bottom.setOpaque(false);

        JPanel actions = createRoundedPanel(CARD, new EmptyBorder(12, 16, 12, 16));
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton clearButton = createSecondaryButton("입력 초기화");
        clearButton.addActionListener(e -> clearCurrentGradeInputs());

        JButton gradeButton = createPrimaryButton("채점하기", BTN_GREEN);
        gradeButton.addActionListener(e -> performUnifiedGrading());

        actions.add(clearButton);
        actions.add(gradeButton);

        JPanel resultCard = createRoundedPanel(CARD, new EmptyBorder(12, 14, 12, 14));
        resultCard.setLayout(new BorderLayout(8, 8));

        JLabel resultTitle = new JLabel("채점 결과");
        resultTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        resultTitle.setForeground(TEXT);

        gradeResultArea = new JTextArea(4, 20);
        gradeResultArea.setEditable(false);
        gradeResultArea.setLineWrap(true);
        gradeResultArea.setWrapStyleWord(true);
        gradeResultArea.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        gradeResultArea.setMargin(new Insets(10, 10, 10, 10));
        gradeResultArea.setBackground(CARD_SOFT);
        gradeResultArea.setForeground(TEXT);
        gradeResultArea.setBorder(new LineBorder(LINE));

        JScrollPane resultScroll = new JScrollPane(gradeResultArea);
        resultScroll.setPreferredSize(new Dimension(100, 120));
        resultScroll.setBorder(BorderFactory.createEmptyBorder());

        resultCard.add(resultTitle, BorderLayout.NORTH);
        resultCard.add(resultScroll, BorderLayout.CENTER);

        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(resultCard, BorderLayout.CENTER);
        return bottom;
    }

    private void rebuildGradeScreen() {
        if (gradeDynamicContainer == null) return;

        gradeResultArea.setText("");
        gradePathLabel.setText("정답 파일: " + toShortPath(ANSWERS_FILE.toAbsolutePath().toString(), 80));
        gradeDynamicContainer.removeAll();
        gradeLevelCombo.setVisible(currentGradeTool == ToolType.MONTHLY);

        if (currentGradeTool == ToolType.MONTHLY) {
            gradeToolLabel.setText("JET 채점");
            String level = (String) gradeLevelCombo.getSelectedItem();
            MonthlyLevelConfig cfg = monthlyConfig.levels.get(level);
            gradeSummaryLabel.setText("현재 레벨: " + level + " | LC " + cfg.lcParts.size() + "개 파트 / RC " + cfg.rcParts.size() + "개 파트");
            gradeDynamicContainer.add(buildMonthlyGradePanel(cfg), BorderLayout.CENTER);
        } else if (currentGradeTool == ToolType.MOCK) {
            gradeToolLabel.setText("모의고사 채점");
            gradeSummaryLabel.setText("총 45문항 | LC 1~17 / RC 18~45 | 6 = 무효 답안");
            gradeDynamicContainer.add(buildMockGradePanel(), BorderLayout.CENTER);
        } else {
            gradeToolLabel.setText("학업성취도 평가 채점");
            gradeSummaryLabel.setText("LC 객관 → LC 서답 → RC 객관 → RC 서답 순서 채점");
            gradeDynamicContainer.add(buildAchievementGradePanel(), BorderLayout.CENTER);
        }

        gradeDynamicContainer.revalidate();
        gradeDynamicContainer.repaint();
    }

    private JPanel buildMonthlyGradePanel(MonthlyLevelConfig cfg) {
        monthlyGradeLcFields.clear();
        monthlyGradeRcFields.clear();

        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);

        JPanel lc = createCardSection("LC 입력");
        JPanel rc = createCardSection("RC 입력");
        buildMonthlyRows(lc, monthlyGradeLcFields, cfg.lcParts, true, null);
        buildMonthlyRows(rc, monthlyGradeRcFields, cfg.rcParts, true, null);

        center.add(createStyledScrollPane(lc));
        center.add(createStyledScrollPane(rc));
        focusFirstMonthlyField();
        return center;
    }

    private JPanel buildMockGradePanel() {
        mockGradeFields.clear();

        JPanel wrapper = new JPanel(new BorderLayout(18, 18));
        wrapper.setOpaque(false);

        JPanel topCard = createRoundedPanel(CARD, new EmptyBorder(16, 18, 16, 18));
        topCard.setLayout(new BorderLayout());
        JLabel info = new JLabel("1~5는 답안 선택, 6은 무효 답안입니다. 숫자 1개를 입력하면 다음 칸으로 자동 이동합니다.");
        info.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        info.setForeground(MUTED);
        topCard.add(info, BorderLayout.WEST);
        wrapper.add(topCard, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel lcPanel = buildMockSection("LC 1~17", 1, 17);
        lcPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rcPanel = buildMockSection("RC 18~45", 18, 45);
        rcPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(lcPanel);
        content.add(Box.createVerticalStrut(18));
        content.add(rcPanel);

        wrapper.add(createStyledScrollPane(content), BorderLayout.CENTER);

        if (!mockGradeFields.isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    mockGradeFields.get(0).requestFocusInWindow();
                }
            });
        }
        return wrapper;
    }

    private JPanel buildMockSection(String title, int start, int end) {
        JPanel card = createCardSection(title);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        flow.setOpaque(false);

        int count = end - start + 1;
        List<DigitField> local = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DigitField f = new DigitField(6);
            local.add(f);
            mockGradeFields.add(f);
        }
        wireAutoMove(local);

        for (int i = 0; i < count; i++) {
            JPanel wrap = new JPanel(new BorderLayout(0, 4));
            wrap.setOpaque(false);

            JLabel num = new JLabel(String.valueOf(start + i), SwingConstants.CENTER);
            num.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            num.setForeground(MUTED);

            wrap.add(local.get(i), BorderLayout.CENTER);
            wrap.add(num, BorderLayout.SOUTH);
            flow.add(wrap);
        }

        card.add(flow);
        return card;
    }

    private JPanel buildAchievementGradePanel() {
        achLcMcFields.clear();
        achLcSaFields.clear();
        achRcMcFields.clear();
        achRcSaFields.clear();

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel lcMc = buildAchievementMcSection("LC 객관 1~15", 1, 15, achLcMcFields, 6);
        lcMc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel lcSa = buildAchievementSaSection("LC 서답 1~3", achievementConfig.lcSaIndex, achLcSaFields);
        lcSa.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rcMc = buildAchievementMcSection("RC 객관 16~34", 16, 34, achRcMcFields, 6);
        rcMc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rcSa = buildAchievementSaSection("RC 서답 4~6", achievementConfig.rcSaIndex, achRcSaFields);
        rcSa.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(lcMc);
        content.add(Box.createVerticalStrut(18));
        content.add(lcSa);
        content.add(Box.createVerticalStrut(18));
        content.add(rcMc);
        content.add(Box.createVerticalStrut(18));
        content.add(rcSa);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(createStyledScrollPane(content), BorderLayout.CENTER);

        if (!achLcMcFields.isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    achLcMcFields.get(0).requestFocusInWindow();
                }
            });
        }
        return wrapper;
    }
    private JPanel buildAchievementMcSection(String title, int start, int end, List<DigitField> target, int maxDigit) {
        JPanel card = createCardSection(title);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        flow.setOpaque(false);

        List<DigitField> local = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            DigitField f = new DigitField(maxDigit);
            local.add(f);
            target.add(f);
        }
        wireAutoMove(local);

        for (int i = 0; i < local.size(); i++) {
            JPanel wrap = new JPanel(new BorderLayout(0, 4));
            wrap.setOpaque(false);

            JLabel num = new JLabel(String.valueOf(start + i), SwingConstants.CENTER);
            num.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            num.setForeground(MUTED);

            wrap.add(local.get(i), BorderLayout.CENTER);
            wrap.add(num, BorderLayout.SOUTH);
            flow.add(wrap);
        }

        card.add(flow);
        return card;
    }

    private JPanel buildAchievementSaSection(String title, List<Integer> indices, List<OXField> target) {
        JPanel card = createCardSection(title);
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        flow.setOpaque(false);

        List<OXField> local = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            OXField f = new OXField();
            local.add(f);
            target.add(f);
        }
        wireAutoMoveOX(local);

        for (int i = 0; i < local.size(); i++) {
            JPanel wrap = createRoundedPanel(CARD_SOFT, new EmptyBorder(12, 12, 12, 12));
            wrap.setLayout(new BorderLayout(0, 6));
            JLabel num = new JLabel("서답 " + indices.get(i), SwingConstants.CENTER);
            num.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
            num.setForeground(TEXT);
            wrap.add(num, BorderLayout.NORTH);
            wrap.add(local.get(i), BorderLayout.CENTER);
            flow.add(wrap);
        }

        card.add(flow);
        return card;
    }

    private JPanel buildEditScreen() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        panel.setBackground(BG);
        panel.add(buildEditTopBar(), BorderLayout.NORTH);
        panel.add(buildEditCenter(), BorderLayout.CENTER);
        panel.add(buildEditBottom(), BorderLayout.SOUTH);
        rebuildEditScreen();
        return panel;
    }

    private JPanel buildEditTopBar() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel top = createRoundedPanel(CARD, new EmptyBorder(18, 20, 18, 20));
        top.setLayout(new BorderLayout(16, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JLabel label = new JLabel("답안 수정 항목");
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        label.setForeground(TEXT);

        editToolCombo = new JComboBox<>(ToolType.displayNames());
        styleComboBox(editToolCombo);
        editToolCombo.addActionListener(e -> rebuildEditScreen());

        editLevelCombo = new JComboBox<>(new String[]{"초급", "중급", "고급"});
        styleComboBox(editLevelCombo);
        editLevelCombo.addActionListener(e -> rebuildEditScreen());

        editInfoLabel = new JLabel();
        editInfoLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        editInfoLabel.setForeground(MUTED);

        left.add(label);
        left.add(editToolCombo);
        left.add(editLevelCombo);
        left.add(editInfoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton homeButton = createSecondaryButton("메인으로");
        homeButton.addActionListener(e -> showScreen(SCREEN_HOME));
        right.add(homeButton);
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JPanel sub = createRoundedPanel(CARD_SOFT, new EmptyBorder(14, 18, 14, 18));
        sub.setLayout(new BorderLayout());
        editPathLabel = createMetaLabel();
        sub.add(editPathLabel, BorderLayout.WEST);

        wrapper.add(top);
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(sub);
        return wrapper;
    }

    private JPanel buildEditCenter() {
        editDynamicContainer = new JPanel(new BorderLayout());
        editDynamicContainer.setOpaque(false);
        return editDynamicContainer;
    }

    private JPanel buildEditBottom() {
        JPanel bottom = createRoundedPanel(CARD, new EmptyBorder(12, 16, 12, 16));
        bottom.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton reloadButton = createSecondaryButton("파일 다시 불러오기");
        reloadButton.addActionListener(e -> reloadAnswersFromDisk());

        JButton resetButton = createSecondaryButton("현재값 다시 불러오기");
        resetButton.addActionListener(e -> rebuildEditScreen());

        JButton saveButton = createPrimaryButton("저장하기", SUCCESS);
        saveButton.addActionListener(e -> saveEditedAnswers());

        bottom.add(reloadButton);
        bottom.add(resetButton);
        bottom.add(saveButton);
        return bottom;
    }

    private void rebuildEditScreen() {
        if (editDynamicContainer == null) return;

        ToolType type = ToolType.fromDisplay((String) editToolCombo.getSelectedItem());
        editLevelCombo.setVisible(type == ToolType.MONTHLY);
        editPathLabel.setText("저장 위치: " + toShortPath(ANSWERS_FILE.toAbsolutePath().toString(), 95));
        editDynamicContainer.removeAll();

        if (type == ToolType.MONTHLY) {
            String level = (String) editLevelCombo.getSelectedItem();
            editInfoLabel.setText("JET " + level + " 답안 수정");
            editDynamicContainer.add(buildMonthlyEditPanel(monthlyConfig.levels.get(level)), BorderLayout.CENTER);
        } else if (type == ToolType.MOCK) {
            editInfoLabel.setText("모의고사 정답 및 3점 문항 수정");
            editDynamicContainer.add(buildMockEditPanel(), BorderLayout.CENTER);
        } else {
            editInfoLabel.setText("학업성취도 평가 객관식 정답 수정");
            editDynamicContainer.add(buildAchievementEditPanel(), BorderLayout.CENTER);
        }

        editDynamicContainer.revalidate();
        editDynamicContainer.repaint();
    }

    private JPanel buildMonthlyEditPanel(MonthlyLevelConfig cfg) {
        monthlyEditLcFields.clear();
        monthlyEditRcFields.clear();

        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);
        JPanel lc = createCardSection("LC 정답 수정");
        JPanel rc = createCardSection("RC 정답 수정");
        buildMonthlyRows(lc, monthlyEditLcFields, cfg.lcParts, false, cfg.lcAnswers);
        buildMonthlyRows(rc, monthlyEditRcFields, cfg.rcParts, false, cfg.rcAnswers);
        center.add(createStyledScrollPane(lc));
        center.add(createStyledScrollPane(rc));
        return center;
    }

    private JPanel buildMockEditPanel() {
        mockEditAnswerFields.clear();

        JPanel wrapper = new JPanel(new BorderLayout(18, 18));
        wrapper.setOpaque(false);

        JPanel top = createRoundedPanel(CARD, new EmptyBorder(16, 18, 16, 18));
        top.setLayout(new GridLayout(2, 1, 8, 8));
        JLabel info1 = new JLabel("모의고사 정답은 1~5만 입력합니다.");
        info1.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        info1.setForeground(MUTED);
        JPanel threePointPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        threePointPanel.setOpaque(false);
        JLabel tpLabel = new JLabel("3점 문항 번호 (쉼표 구분)");
        tpLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        mockThreePointField = new JTextField(joinIntSet(mockConfig.threePointQuestions), 30);
        mockThreePointField.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        threePointPanel.add(tpLabel);
        threePointPanel.add(mockThreePointField);
        top.add(info1);
        top.add(threePointPanel);

        JPanel fieldsCard = createCardSection("정답 1~45");
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        flow.setOpaque(false);
        List<DigitField> local = new ArrayList<>();
        for (int i = 0; i < mockConfig.answerKey.size(); i++) {
            DigitField f = new DigitField(5);
            f.setText(mockConfig.answerKey.get(i));
            local.add(f);
            mockEditAnswerFields.add(f);
        }
        wireAutoMove(local);
        for (int i = 0; i < local.size(); i++) {
            JPanel wrap = new JPanel(new BorderLayout(0, 4));
            wrap.setOpaque(false);
            JLabel num = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
            num.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
            num.setForeground(MUTED);
            wrap.add(local.get(i), BorderLayout.CENTER);
            wrap.add(num, BorderLayout.SOUTH);
            flow.add(wrap);
        }
        fieldsCard.add(flow);

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(createStyledScrollPane(fieldsCard), BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildAchievementEditPanel() {
        achEditLcFields.clear();
        achEditRcFields.clear();

        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);
        center.add(buildAchievementAnswerEditSection("LC 객관 정답 1~15", achievementConfig.lcMcKey, achEditLcFields, 1));
        center.add(buildAchievementAnswerEditSection("RC 객관 정답 16~34", achievementConfig.rcMcKey, achEditRcFields, 16));
        return center;
    }

    private JPanel buildAchievementAnswerEditSection(String title, List<String> values, List<DigitField> target, int startNum) {
        JPanel card = createCardSection(title);
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        flow.setOpaque(false);
        List<DigitField> local = new ArrayList<>();
        for (String value : values) {
            DigitField f = new DigitField(5);
            f.setText(value);
            local.add(f);
            target.add(f);
        }
        wireAutoMove(local);
        for (int i = 0; i < local.size(); i++) {
            JPanel wrap = new JPanel(new BorderLayout(0, 4));
            wrap.setOpaque(false);
            JLabel num = new JLabel(String.valueOf(startNum + i), SwingConstants.CENTER);
            num.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
            num.setForeground(MUTED);
            wrap.add(local.get(i), BorderLayout.CENTER);
            wrap.add(num, BorderLayout.SOUTH);
            flow.add(wrap);
        }
        card.add(flow);
        return createStyledScrollPane(card).getViewport().getView() instanceof JPanel ? card : card;
    }

    private void buildMonthlyRows(JPanel container, Map<String, List<DigitField>> fieldMap, List<PartConfig> parts, boolean studentMode, Map<String, List<String>> presetMap) {
        for (PartConfig part : parts) {
            JPanel partPanel = createPartPanel(part, !studentMode);
            JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            flow.setOpaque(false);

            List<DigitField> fields = new ArrayList<>();
            for (int i = 0; i < part.count; i++) {
                DigitField f = new DigitField(5);
                if (!studentMode && presetMap != null) {
                    f.setText(presetMap.get(part.name).get(i));
                }
                fields.add(f);
            }
            wireAutoMove(fields);
            fieldMap.put(part.name, fields);

            for (int i = 0; i < fields.size(); i++) {
                JPanel wrap = new JPanel(new BorderLayout(0, 4));
                wrap.setOpaque(false);
                JLabel num = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
                num.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
                num.setForeground(MUTED);
                wrap.add(fields.get(i), BorderLayout.CENTER);
                wrap.add(num, BorderLayout.SOUTH);
                flow.add(wrap);
            }

            partPanel.add(flow, BorderLayout.CENTER);
            container.add(partPanel);
            container.add(Box.createVerticalStrut(12));
        }
        container.add(Box.createVerticalGlue());
    }

    private JPanel createPartPanel(PartConfig part, boolean editMode) {
        JPanel partPanel = createRoundedPanel(CARD_SOFT, new EmptyBorder(14, 14, 14, 14));
        partPanel.setLayout(new BorderLayout(10, 12));
        partPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel partLabel = new JLabel(editMode ? part.name + "  (" + part.count + "문항)" : part.name + "  (" + part.count + "문항 / 배점 " + formatNumber(part.weight) + ")");
        partLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        partLabel.setForeground(TEXT);
        JLabel side = new JLabel(editMode ? "정답 입력" : "학생 답안 입력");
        side.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        side.setForeground(MUTED);
        header.add(partLabel, BorderLayout.WEST);
        header.add(side, BorderLayout.EAST);
        partPanel.add(header, BorderLayout.NORTH);
        return partPanel;
    }

    private void clearCurrentGradeInputs() {
        gradeResultArea.setText("");
        if (currentGradeTool == ToolType.MONTHLY) {
            clearFieldMap(monthlyGradeLcFields);
            clearFieldMap(monthlyGradeRcFields);
            focusFirstMonthlyField();
        } else if (currentGradeTool == ToolType.MOCK) {
            for (DigitField f : mockGradeFields) f.setText("");
            if (!mockGradeFields.isEmpty()) mockGradeFields.get(0).requestFocusInWindow();
        } else {
            for (DigitField f : achLcMcFields) f.setText("");
            for (DigitField f : achRcMcFields) f.setText("");
            for (OXField f : achLcSaFields) f.setText("");
            for (OXField f : achRcSaFields) f.setText("");
            if (!achLcMcFields.isEmpty()) achLcMcFields.get(0).requestFocusInWindow();
        }
    }

    private void focusFirstMonthlyField() {
        SwingUtilities.invokeLater(() -> {
            for (List<DigitField> list : monthlyGradeLcFields.values()) {
                if (!list.isEmpty()) {
                    list.get(0).requestFocusInWindow();
                    break;
                }
            }
        });
    }

    private void clearFieldMap(Map<String, List<DigitField>> map) {
        for (List<DigitField> list : map.values()) {
            for (DigitField f : list) f.setText("");
        }
    }

    private void performUnifiedGrading() {
        if (currentGradeTool == ToolType.MONTHLY) {
            performMonthlyGrading();
        } else if (currentGradeTool == ToolType.MOCK) {
            performMockGrading();
        } else {
            performAchievementGrading();
        }
    }

    private void performMonthlyGrading() {
        String levelName = (String) gradeLevelCombo.getSelectedItem();
        MonthlyLevelConfig cfg = monthlyConfig.levels.get(levelName);

        GroupScore lcScore = scoreMonthlyGroup(cfg.lcParts, cfg.lcAnswers, monthlyGradeLcFields, "LC");
        if (lcScore == null) return;
        GroupScore rcScore = scoreMonthlyGroup(cfg.rcParts, cfg.rcAnswers, monthlyGradeRcFields, "RC");
        if (rcScore == null) return;

        double lc = cfg.baseLC + lcScore.score;
        double rc = cfg.baseRC + rcScore.score;
        double total = lc + rc;
        double lcMax = cfg.baseLC + calculateMax(cfg.lcParts);
        double rcMax = cfg.baseRC + calculateMax(cfg.rcParts);

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(levelName).append(" JET]   ");
        sb.append("LC ").append(formatNumber(lc)).append("/").append(formatNumber(lcMax));
        sb.append("   |   RC ").append(formatNumber(rc)).append("/").append(formatNumber(rcMax));
        sb.append("   |   총점 ").append(formatNumber(total)).append("/").append(formatNumber(lcMax + rcMax));

        sb.append("\n\nLC 세부 → ");
        for (int i = 0; i < lcScore.summaries.size(); i++) {
            PartSummary s = lcScore.summaries.get(i);
            if (i > 0) sb.append("   |   ");
            sb.append(s.partName).append(": ").append(s.correctCount).append("/").append(s.totalCount);
        }

        sb.append("\nRC 세부 → ");
        for (int i = 0; i < rcScore.summaries.size(); i++) {
            PartSummary s = rcScore.summaries.get(i);
            if (i > 0) sb.append("   |   ");
            sb.append(s.partName).append(": ").append(s.correctCount).append("/").append(s.totalCount);
        }

        gradeResultArea.setText(sb.toString());
    }

    private GroupScore scoreMonthlyGroup(List<PartConfig> parts, Map<String, List<String>> answers, Map<String, List<DigitField>> inputs, String label) {
        double score = 0.0;
        List<PartSummary> summaries = new ArrayList<>();
        for (PartConfig part : parts) {
            List<DigitField> fields = inputs.get(part.name);
            List<String> correct = answers.get(part.name);
            int got = 0;
            for (int i = 0; i < part.count; i++) {
                String value = fields.get(i).getText().trim();
                if (!value.matches("[1-5]")) {
                    JOptionPane.showMessageDialog(this, label + " " + part.name + "의 " + (i + 1) + "번 칸을 확인하세요.\n1~5 중 하나만 입력해야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                    fields.get(i).requestFocusInWindow();
                    return null;
                }
                if (value.equals(correct.get(i))) got++;
            }
            score += got * part.weight;
            summaries.add(new PartSummary(part.name, got, part.count));
        }
        return new GroupScore(score, summaries);
    }

    private void performMockGrading() {
        if (mockGradeFields.size() != 45) return;
        List<String> student = new ArrayList<>();
        for (int i = 0; i < mockGradeFields.size(); i++) {
            String value = mockGradeFields.get(i).getText().trim();
            if (!value.matches("[1-6]")) {
                JOptionPane.showMessageDialog(this, (i + 1) + "번 답안을 확인하세요.\n1~6 중 하나만 입력해야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                mockGradeFields.get(i).requestFocusInWindow();
                return;
            }
            student.add(value);
        }

        double lcScore = 0.0;
        double rcScore = 0.0;
        int lcGot = 0;
        int rcGot = 0;
        List<Integer> lcWrong = new ArrayList<>();
        List<Integer> rcWrong = new ArrayList<>();
        List<Double> weights = mockConfig.buildWeights();

        for (int i = 0; i < 45; i++) {
            int qnum = i + 1;
            String stu = student.get(i);
            if (stu.equals("6")) {
                if (qnum <= 17) lcWrong.add(qnum); else rcWrong.add(qnum);
            } else if (stu.equals(mockConfig.answerKey.get(i))) {
                if (qnum <= 17) {
                    lcGot++;
                    lcScore += weights.get(i);
                } else {
                    rcGot++;
                    rcScore += weights.get(i);
                }
            } else {
                if (qnum <= 17) lcWrong.add(qnum); else rcWrong.add(qnum);
            }
        }

        double lcMax = 0.0;
        for (int i = 0; i < 17; i++) lcMax += weights.get(i);
        double rcMax = 0.0;
        for (int i = 17; i < 45; i++) rcMax += weights.get(i);

        StringBuilder sb = new StringBuilder();
        sb.append("[모의고사]   ");
        sb.append("LC ").append(formatNumber(lcScore)).append("/").append(formatNumber(lcMax)).append(" (").append(lcGot).append("/17)");
        sb.append("   |   RC ").append(formatNumber(rcScore)).append("/").append(formatNumber(rcMax)).append(" (").append(rcGot).append("/28)");
        sb.append("   |   총점 ").append(formatNumber(lcScore + rcScore)).append("/").append(formatNumber(lcMax + rcMax));

        sb.append("\n\n오답/무효 → LC: ").append(lcWrong.isEmpty() ? "없음" : joinIntegers(lcWrong));
        sb.append("   |   RC: ").append(rcWrong.isEmpty() ? "없음" : joinIntegers(rcWrong));
        sb.append("\n3점 문항 → ").append(joinIntSet(mockConfig.threePointQuestions));

        gradeResultArea.setText(sb.toString());
    }

    private void performAchievementGrading() {
        List<String> lcMc = readDigitFields(achLcMcFields, 1, "LC 객관");
        if (lcMc == null) return;
        List<String> rcMc = readDigitFields(achRcMcFields, 16, "RC 객관");
        if (rcMc == null) return;
        List<String> lcSa = readOXFields(achLcSaFields, achievementConfig.lcSaIndex, "LC 서답");
        if (lcSa == null) return;
        List<String> rcSa = readOXFields(achRcSaFields, achievementConfig.rcSaIndex, "RC 서답");
        if (rcSa == null) return;

        List<Integer> lcMcWrong = scoreWrongNumbers(lcMc, achievementConfig.lcMcKey, 1);
        List<Integer> rcMcWrong = scoreWrongNumbers(rcMc, achievementConfig.rcMcKey, 16);
        List<Integer> lcSaWrong = new ArrayList<>();
        for (int i = 0; i < lcSa.size(); i++) if (lcSa.get(i).equals("X")) lcSaWrong.add(achievementConfig.lcSaIndex.get(i));
        List<Integer> rcSaWrong = new ArrayList<>();
        for (int i = 0; i < rcSa.size(); i++) if (rcSa.get(i).equals("X")) rcSaWrong.add(achievementConfig.rcSaIndex.get(i));

        int lcScore = Math.max(0, achievementConfig.lcMax - (lcMcWrong.size() * 2 + lcSaWrong.size() * 4));
        int rcScore = Math.max(0, achievementConfig.rcMax - (rcMcWrong.size() * 2 + rcSaWrong.size() * 4));

        StringBuilder sb = new StringBuilder();
        sb.append("[학업성취도 평가]   ");
        sb.append("LC ").append(lcScore).append("/").append(achievementConfig.lcMax);
        sb.append("   |   RC ").append(rcScore).append("/").append(achievementConfig.rcMax);
        sb.append("   |   총점 ").append(lcScore + rcScore).append("/").append(achievementConfig.lcMax + achievementConfig.rcMax);

        sb.append("\n\nLC 오답 → 객관: ").append(lcMcWrong.isEmpty() ? "없음" : joinIntegers(lcMcWrong));
        sb.append("   |   서답: ").append(lcSaWrong.isEmpty() ? "없음" : joinIntegers(lcSaWrong));
        sb.append("\nRC 오답 → 객관: ").append(rcMcWrong.isEmpty() ? "없음" : joinIntegers(rcMcWrong));
        sb.append("   |   서답: ").append(rcSaWrong.isEmpty() ? "없음" : joinIntegers(rcSaWrong));

        gradeResultArea.setText(sb.toString());
    }

    private List<String> readDigitFields(List<DigitField> fields, int startNum, String label) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            String value = fields.get(i).getText().trim();
            if (!value.matches("[1-6]")) {
                JOptionPane.showMessageDialog(this, label + " " + (startNum + i) + "번을 확인하세요.\n1~6 중 하나만 입력해야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                fields.get(i).requestFocusInWindow();
                return null;
            }
            result.add(value);
        }
        return result;
    }

    private List<String> readOXFields(List<OXField> fields, List<Integer> indices, String label) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            String value = fields.get(i).getText().trim().toUpperCase(Locale.ROOT);
            if (!value.matches("[OX]")) {
                JOptionPane.showMessageDialog(this, label + " " + indices.get(i) + "번을 확인하세요.\nO 또는 X만 입력해야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                fields.get(i).requestFocusInWindow();
                return null;
            }
            result.add(value);
        }
        return result;
    }

    private List<Integer> scoreWrongNumbers(List<String> stu, List<String> key, int startNum) {
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < stu.size(); i++) {
            if (!stu.get(i).equals(key.get(i))) wrong.add(startNum + i);
        }
        return wrong;
    }

    private void saveEditedAnswers() {
        ToolType type = ToolType.fromDisplay((String) editToolCombo.getSelectedItem());
        if (type == ToolType.MONTHLY) {
            String level = (String) editLevelCombo.getSelectedItem();
            MonthlyLevelConfig cfg = monthlyConfig.levels.get(level);
            if (!applyMonthlyEdit(cfg.lcParts, cfg.lcAnswers, monthlyEditLcFields, "LC")) return;
            if (!applyMonthlyEdit(cfg.rcParts, cfg.rcAnswers, monthlyEditRcFields, "RC")) return;
        } else if (type == ToolType.MOCK) {
            if (!applyMockEdit()) return;
        } else {
            if (!applyAchievementEdit()) return;
        }

        if (!saveAnswersToJsonWithAlert(true)) return;
        JOptionPane.showMessageDialog(this, "답안이 저장되었습니다.\nanswers.json 파일도 함께 갱신되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
        rebuildGradeScreen();
        rebuildEditScreen();
        showScreen(SCREEN_HOME);
    }

    private boolean applyMonthlyEdit(List<PartConfig> parts, Map<String, List<String>> target, Map<String, List<DigitField>> editMap, String label) {
        for (PartConfig part : parts) {
            List<String> values = new ArrayList<>();
            List<DigitField> fields = editMap.get(part.name);
            for (int i = 0; i < fields.size(); i++) {
                String value = fields.get(i).getText().trim();
                if (!value.matches("[1-5]")) {
                    JOptionPane.showMessageDialog(this, label + " " + part.name + "의 " + (i + 1) + "번 정답을 확인하세요.\n1~5만 허용됩니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                    fields.get(i).requestFocusInWindow();
                    return false;
                }
                values.add(value);
            }
            target.put(part.name, values);
        }
        return true;
    }

    private boolean applyMockEdit() {
        List<String> newKey = new ArrayList<>();
        for (int i = 0; i < mockEditAnswerFields.size(); i++) {
            String value = mockEditAnswerFields.get(i).getText().trim();
            if (!value.matches("[1-5]")) {
                JOptionPane.showMessageDialog(this, (i + 1) + "번 정답을 확인하세요.\n모의고사 정답은 1~5만 허용됩니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                mockEditAnswerFields.get(i).requestFocusInWindow();
                return false;
            }
            newKey.add(value);
        }

        Set<Integer> threePoint = parseThreePointField(mockThreePointField.getText().trim());
        if (threePoint == null) {
            JOptionPane.showMessageDialog(this, "3점 문항 번호 형식을 확인하세요.\n예: 6,13,14,29", "입력 오류", JOptionPane.ERROR_MESSAGE);
            mockThreePointField.requestFocusInWindow();
            return false;
        }

        mockConfig.answerKey = newKey;
        mockConfig.threePointQuestions = threePoint;
        return true;
    }

    private boolean applyAchievementEdit() {
        List<String> lc = new ArrayList<>();
        List<String> rc = new ArrayList<>();

        for (int i = 0; i < achEditLcFields.size(); i++) {
            String value = achEditLcFields.get(i).getText().trim();
            if (!value.matches("[1-5]")) {
                JOptionPane.showMessageDialog(this, "LC 객관 " + (i + 1) + "번 정답을 확인하세요.\n1~5만 허용됩니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                achEditLcFields.get(i).requestFocusInWindow();
                return false;
            }
            lc.add(value);
        }
        for (int i = 0; i < achEditRcFields.size(); i++) {
            String value = achEditRcFields.get(i).getText().trim();
            if (!value.matches("[1-5]")) {
                JOptionPane.showMessageDialog(this, "RC 객관 " + (16 + i) + "번 정답을 확인하세요.\n1~5만 허용됩니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                achEditRcFields.get(i).requestFocusInWindow();
                return false;
            }
            rc.add(value);
        }
        achievementConfig.lcMcKey = lc;
        achievementConfig.rcMcKey = rc;
        return true;
    }

    private Set<Integer> parseThreePointField(String text) {
        Set<Integer> result = new LinkedHashSet<>();
        if (text.isBlank()) return result;
        String[] parts = text.split(",");
        try {
            for (String p : parts) {
                int n = Integer.parseInt(p.trim());
                if (n < 1 || n > 45) return null;
                result.add(n);
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void reloadAnswersFromDisk() {
        try {
            if (!Files.exists(ANSWERS_FILE)) {
                JOptionPane.showMessageDialog(this, "answers.json 파일이 아직 없습니다.\n기본값을 먼저 저장합니다.", "파일 없음", JOptionPane.WARNING_MESSAGE);
                saveAnswersToJsonWithAlert(true);
            } else {
                loadAnswersFromJson();
                rebuildGradeScreen();
                rebuildEditScreen();
                JOptionPane.showMessageDialog(this, "answers.json 파일을 다시 불러왔습니다.", "불러오기 완료", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "answers.json 파일을 다시 불러오지 못했습니다.\n사유: " + e.getMessage(), "불러오기 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean saveAnswersToJsonWithAlert(boolean showError) {
        try {
            Files.writeString(ANSWERS_FILE, buildAnswersJson(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return true;
        } catch (IOException e) {
            if (showError) {
                JOptionPane.showMessageDialog(this, "answers.json 저장에 실패했습니다.\n사유: " + e.getMessage(), "저장 실패", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    private String buildAnswersJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 2,\n");
        sb.append("  \"monthly\": ").append(monthlyConfig.toJson(2)).append(",\n");
        sb.append("  \"mock\": ").append(mockConfig.toJson(2)).append(",\n");
        sb.append("  \"achievement\": ").append(achievementConfig.toJson(2)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private void loadAnswersFromJson() throws IOException {
        String json = Files.readString(ANSWERS_FILE, StandardCharsets.UTF_8);
        Object parsed = new SimpleJsonParser(json).parse();
        if (!(parsed instanceof Map)) throw new IOException("JSON 최상위 구조가 객체가 아닙니다.");

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parsed;
        Object monthlyNode = root.get("monthly");
        if (monthlyNode instanceof Map) monthlyConfig.applyFromJson((Map<String, Object>) monthlyNode);
        Object mockNode = root.get("mock");
        if (mockNode instanceof Map) mockConfig.applyFromJson((Map<String, Object>) mockNode);
        Object achNode = root.get("achievement");
        if (achNode instanceof Map) achievementConfig.applyFromJson((Map<String, Object>) achNode);
    }

    private JPanel createCardSection(String title) {
        JPanel section = createRoundedPanel(CARD, new EmptyBorder(16, 16, 16, 16));
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        titleLabel.setForeground(TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(14));
        return section;
    }

    private JScrollPane createStyledScrollPane(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setBackground(BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createRoundedPanel(Color bg, Border padding) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), padding));
        return panel;
    }

    private JLabel createBadge(String text) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(new Color(233, 239, 255));
        badge.setForeground(PRIMARY_DARK);
        badge.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        badge.setBorder(new CompoundBorder(new LineBorder(new Color(206, 219, 255), 1, true), new EmptyBorder(6, 10, 6, 10)));
        return badge;
    }

    private JPanel createTinyInfoCard(String title, String value) {
        JPanel panel = createRoundedPanel(CARD_SOFT, new EmptyBorder(12, 14, 12, 14));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        t.setForeground(MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        v.setForeground(TEXT);
        panel.add(t);
        panel.add(Box.createVerticalStrut(4));
        panel.add(v);
        return panel;
    }

    private JPanel createHomeActionCard(String title, String description, String meta, Color accent, ActionListener action) {
        JPanel card = createRoundedPanel(CARD, new EmptyBorder(22, 22, 22, 22));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        titleLabel.setForeground(TEXT);
        JLabel descLabel = new JLabel(toMultilineHtml(description, 280));
        descLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        descLabel.setForeground(MUTED);
        JLabel metaLabel = new JLabel(meta);
        metaLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        metaLabel.setForeground(accent.darker());
        JButton button = createPrimaryButton("열기", BTN_BLUE);
        button.addActionListener(action);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(descLabel);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(16));
        card.add(metaLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(button);
        return card;
    }

    private JButton createPrimaryButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);

        btn.setBackground(color);
        btn.setOpaque(true);
        btn.setBorderPainted(false);

        btn.setPreferredSize(new Dimension(140, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // hover 효과
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.darker());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });

        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));

        btn.setBackground(new Color(120, 130, 150));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);

        btn.setPreferredSize(new Dimension(120, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(100, 110, 130));
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(120, 130, 150));
            }
        });

        return btn;
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        comboBox.setPreferredSize(new Dimension(170, 36));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT);
    }

    private JLabel createMetaLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        label.setForeground(MUTED);
        return label;
    }

    private String toMultilineHtml(String text, int width) {
        return "<html><div style='width:" + width + "px;'>" + text + "</div></html>";
    }

    private String toShortPath(String path, int maxLen) {
        if (path.length() <= maxLen) return path;
        return path.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 1e-9) return String.valueOf((int) Math.round(value));
        return String.format(Locale.US, "%.1f", value);
    }

    private double calculateMax(List<PartConfig> parts) {
        double sum = 0.0;
        for (PartConfig part : parts) sum += part.count * part.weight;
        return sum;
    }

    private String joinIntegers(List<Integer> nums) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nums.get(i));
        }
        return sb.toString();
    }

    private String joinIntSet(Set<Integer> nums) {
        return joinIntegers(new ArrayList<>(nums));
    }

    private void showScreen(String name) {
        cardLayout.show(rootPanel, name);
    }

    private void wireAutoMove(List<? extends JTextField> fields) {
        for (int i = 0; i < fields.size(); i++) {
            JTextField current = fields.get(i);
            JTextField prev = i > 0 ? fields.get(i - 1) : null;
            JTextField next = i < fields.size() - 1 ? fields.get(i + 1) : null;
            if (current instanceof BaseNavField) {
                ((BaseNavField) current).setNavigationTargets(prev, next);
            }
        }
    }

    private void wireAutoMoveOX(List<OXField> fields) {
        wireAutoMove(fields);
    }

    enum ToolType {
        MONTHLY("JET 테스트"),
        MOCK("모의고사"),
        ACHIEVEMENT("학업성취도 평가");

        final String display;

        ToolType(String display) { this.display = display; }

        static String[] displayNames() {
            ToolType[] values = values();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) names[i] = values[i].display;
            return names;
        }

        static ToolType fromDisplay(String display) {
            for (ToolType t : values()) if (t.display.equals(display)) return t;
            return MONTHLY;
        }
    }

    static class PartConfig {
        final String name;
        final int count;
        final double weight;
        PartConfig(String name, int count, double weight) { this.name = name; this.count = count; this.weight = weight; }
    }

    static class PartSummary {
        final String partName;
        final int correctCount;
        final int totalCount;
        PartSummary(String partName, int correctCount, int totalCount) { this.partName = partName; this.correctCount = correctCount; this.totalCount = totalCount; }
    }

    static class GroupScore {
        final double score;
        final List<PartSummary> summaries;
        GroupScore(double score, List<PartSummary> summaries) { this.score = score; this.summaries = summaries; }
    }

    static class MonthlyLevelConfig {
        final String name;
        final double baseLC;
        final double baseRC;
        List<PartConfig> lcParts = new ArrayList<>();
        List<PartConfig> rcParts = new ArrayList<>();
        Map<String, List<String>> lcAnswers = new LinkedHashMap<>();
        Map<String, List<String>> rcAnswers = new LinkedHashMap<>();
        MonthlyLevelConfig(String name, double baseLC, double baseRC) { this.name = name; this.baseLC = baseLC; this.baseRC = baseRC; }
    }

    static class MonthlyExamConfig {
        final Map<String, MonthlyLevelConfig> levels = new LinkedHashMap<>();

        void initializeDefaults() {
            MonthlyLevelConfig beginner = new MonthlyLevelConfig("초급", 4.0, 4.0);
            beginner.lcParts = Arrays.asList(new PartConfig("파트1", 6, 3.0), new PartConfig("파트2", 12, 3.0), new PartConfig("파트3", 10, 3.0), new PartConfig("파트4", 8, 4.0), new PartConfig("파트5", 4, 4.0));
            beginner.rcParts = Arrays.asList(new PartConfig("파트6", 5, 4.0), new PartConfig("파트7", 5, 4.0));
            beginner.lcAnswers.put("파트1", new ArrayList<>(Arrays.asList("2","3","3","2","1","1")));
            beginner.lcAnswers.put("파트2", new ArrayList<>(Arrays.asList("1","3","2","2","1","2","1","2","1","1","3","3")));
            beginner.lcAnswers.put("파트3", new ArrayList<>(Arrays.asList("1","2","3","1","3","1","2","3","1","2")));
            beginner.lcAnswers.put("파트4", new ArrayList<>(Arrays.asList("3","2","3","3","2","2","3","3")));
            beginner.lcAnswers.put("파트5", new ArrayList<>(Arrays.asList("3","1","3","3")));
            beginner.rcAnswers.put("파트6", new ArrayList<>(Arrays.asList("2","2","3","3","1")));
            beginner.rcAnswers.put("파트7", new ArrayList<>(Arrays.asList("1","2","3","1","3")));
            levels.put(beginner.name, beginner);

            MonthlyLevelConfig intermediate = new MonthlyLevelConfig("중급", 0.0, 0.0);
            intermediate.lcParts = Arrays.asList(new PartConfig("파트1", 6, 2.0), new PartConfig("파트2", 10, 2.0), new PartConfig("파트3", 8, 3.0), new PartConfig("파트4", 5, 3.0), new PartConfig("파트5", 6, 3.5));
            intermediate.rcParts = Arrays.asList(new PartConfig("파트6", 6, 4.0), new PartConfig("파트7", 6, 4.0), new PartConfig("파트8", 8, 5.0));
            intermediate.lcAnswers.put("파트1", new ArrayList<>(Arrays.asList("3","3","1","1","2","2")));
            intermediate.lcAnswers.put("파트2", new ArrayList<>(Arrays.asList("1","1","2","3","3","1","1","2","3","2")));
            intermediate.lcAnswers.put("파트3", new ArrayList<>(Arrays.asList("2","1","2","1","1","3","2","1")));
            intermediate.lcAnswers.put("파트4", new ArrayList<>(Arrays.asList("1","2","3","2","1")));
            intermediate.lcAnswers.put("파트5", new ArrayList<>(Arrays.asList("3","1","1","2","3","1")));
            intermediate.rcAnswers.put("파트6", new ArrayList<>(Arrays.asList("2","1","3","1","3","3")));
            intermediate.rcAnswers.put("파트7", new ArrayList<>(Arrays.asList("2","1","3","2","1","3")));
            intermediate.rcAnswers.put("파트8", new ArrayList<>(Arrays.asList("2","2","1","1","2","1","3","3")));
            levels.put(intermediate.name, intermediate);

            MonthlyLevelConfig advanced = new MonthlyLevelConfig("고급", 0.0, 0.0);
            advanced.lcParts = Arrays.asList(new PartConfig("파트1", 6, 2.0), new PartConfig("파트2", 10, 2.0), new PartConfig("파트3", 6, 3.0), new PartConfig("파트4", 8, 3.0), new PartConfig("파트5", 5, 3.5));
            advanced.rcParts = Arrays.asList(new PartConfig("파트6", 8, 3.0), new PartConfig("파트7", 7, 3.5), new PartConfig("파트8", 10, 4.0));
            advanced.lcAnswers.put("파트1", new ArrayList<>(Arrays.asList("1","2","1","2","3","3")));
            advanced.lcAnswers.put("파트2", new ArrayList<>(Arrays.asList("1","1","2","3","1","3","2","2","1","2")));
            advanced.lcAnswers.put("파트3", new ArrayList<>(Arrays.asList("1","2","3","3","2","1")));
            advanced.lcAnswers.put("파트4", new ArrayList<>(Arrays.asList("1","3","2","1","2","3","1","3")));
            advanced.lcAnswers.put("파트5", new ArrayList<>(Arrays.asList("1","2","3","1","1")));
            advanced.rcAnswers.put("파트6", new ArrayList<>(Arrays.asList("2","1","3","3","1","2","1","3")));
            advanced.rcAnswers.put("파트7", new ArrayList<>(Arrays.asList("1","2","3","1","2","3","3")));
            advanced.rcAnswers.put("파트8", new ArrayList<>(Arrays.asList("2","1","3","1","1","3","1","2","2","3")));
            levels.put(advanced.name, advanced);
        }

        String toJson(int indent) {
            String i = " ".repeat(indent);
            String i2 = " ".repeat(indent + 2);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append(i2).append("\"levels\": {\n");
            int idx = 0;
            for (MonthlyLevelConfig level : levels.values()) {
                if (idx++ > 0) sb.append(",\n");
                sb.append(i2).append("  \"").append(level.name).append("\": {\n");
                sb.append(i2).append("    \"LC\": ").append(groupToJson(level.lcParts, level.lcAnswers, indent + 6)).append(",\n");
                sb.append(i2).append("    \"RC\": ").append(groupToJson(level.rcParts, level.rcAnswers, indent + 6)).append("\n");
                sb.append(i2).append("  }");
            }
            sb.append("\n").append(i2).append("}\n");
            sb.append(i).append("}");
            return sb.toString();
        }

        void applyFromJson(Map<String, Object> root) throws IOException {
            Object levelsNode = root.get("levels");
            if (!(levelsNode instanceof Map)) return;
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) levelsNode;
            for (Map.Entry<String, MonthlyLevelConfig> e : levels.entrySet()) {
                Object obj = map.get(e.getKey());
                if (!(obj instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> levelNode = (Map<String, Object>) obj;
                applyGroup(levelNode.get("LC"), e.getValue().lcParts, e.getValue().lcAnswers);
                applyGroup(levelNode.get("RC"), e.getValue().rcParts, e.getValue().rcAnswers);
            }
        }
    }

    static class MockExamConfig {
        List<String> answerKey = new ArrayList<>();
        Set<Integer> threePointQuestions = new LinkedHashSet<>();
        double defaultPoint = 2.0;

        void initializeDefaults() {
            answerKey = new ArrayList<>(Arrays.asList(
                    "4","1","5","5","2","4","2","4","4","2","1","5","1","1","1","2","4",
                    "3","3","2","1","3","2","2","5","3","3","5","2","5","1","3","1","2","4","2","3",
                    "5","5","3","3","3","5","4","4"
            ));
            threePointQuestions = new LinkedHashSet<>(Arrays.asList(6, 13, 14, 29, 32, 33, 34, 35, 39, 42));
        }

        List<Double> buildWeights() {
            List<Double> list = new ArrayList<>();
            for (int i = 1; i <= 45; i++) list.add(threePointQuestions.contains(i) ? 3.0 : defaultPoint);
            return list;
        }

        String toJson(int indent) {
            String i = " ".repeat(indent);
            String i2 = " ".repeat(indent + 2);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append(i2).append("\"answerKey\": ").append(stringListToJson(answerKey)).append(",\n");
            sb.append(i2).append("\"threePointQuestions\": ").append(intListToJson(new ArrayList<>(threePointQuestions))).append("\n");
            sb.append(i).append("}");
            return sb.toString();
        }

        void applyFromJson(Map<String, Object> root) throws IOException {
            Object keyNode = root.get("answerKey");
            if (keyNode instanceof List) {
                @SuppressWarnings("unchecked") List<Object> raw = (List<Object>) keyNode;
                if (raw.size() != 45) throw new IOException("모의고사 정답 개수는 45개여야 합니다.");
                List<String> newKey = new ArrayList<>();
                for (Object o : raw) {
                    String v = String.valueOf(o).trim();
                    if (!v.matches("[1-5]")) throw new IOException("모의고사 정답은 1~5만 허용됩니다.");
                    newKey.add(v);
                }
                answerKey = newKey;
            }
            Object tpNode = root.get("threePointQuestions");
            if (tpNode instanceof List) {
                @SuppressWarnings("unchecked") List<Object> raw = (List<Object>) tpNode;
                Set<Integer> set = new LinkedHashSet<>();
                for (Object o : raw) {
                    int n = ((Number) o).intValue();
                    if (n < 1 || n > 45) throw new IOException("3점 문항 번호 범위 오류");
                    set.add(n);
                }
                threePointQuestions = set;
            }
        }
    }

    static class AchievementExamConfig {
        List<String> lcMcKey = new ArrayList<>();
        List<String> rcMcKey = new ArrayList<>();
        List<Integer> lcSaIndex = Arrays.asList(1, 2, 3);
        List<Integer> rcSaIndex = Arrays.asList(4, 5, 6);
        int lcMax = 46;
        int rcMax = 54;

        void initializeDefaults() {
            lcMcKey = new ArrayList<>(Arrays.asList("2","1","5","4","2","1","4","1","2","2","1","3","1","4","1"));
            rcMcKey = new ArrayList<>(Arrays.asList("4","4","5","2","5","2","4","4","5","3","4","1","3","5","5","3","3","3","5"));
        }

        String toJson(int indent) {
            String i = " ".repeat(indent);
            String i2 = " ".repeat(indent + 2);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append(i2).append("\"lcMcKey\": ").append(stringListToJson(lcMcKey)).append(",\n");
            sb.append(i2).append("\"rcMcKey\": ").append(stringListToJson(rcMcKey)).append("\n");
            sb.append(i).append("}");
            return sb.toString();
        }

        void applyFromJson(Map<String, Object> root) throws IOException {
            Object lcNode = root.get("lcMcKey");
            if (lcNode instanceof List) lcMcKey = objectListToValidatedStringList((List<?>) lcNode, 15);
            Object rcNode = root.get("rcMcKey");
            if (rcNode instanceof List) rcMcKey = objectListToValidatedStringList((List<?>) rcNode, 19);
        }
    }

    static abstract class BaseNavField extends JTextField {
        protected JTextField previousField;
        protected JTextField nextField;

        BaseNavField(int columns) {
            super(columns);
            setHorizontalAlignment(JTextField.CENTER);
            setBackground(Color.WHITE);
            setForeground(TEXT);
            setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), new EmptyBorder(6, 6, 6, 6)));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    setBorder(new CompoundBorder(new LineBorder(PRIMARY, 2, true), new EmptyBorder(5, 5, 5, 5)));
                    selectAll();
                }
                @Override public void focusLost(FocusEvent e) {
                    setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), new EmptyBorder(6, 6, 6, 6)));
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && getText().isEmpty() && previousField != null) {
                        previousField.requestFocusInWindow();
                        previousField.setText("");
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT && previousField != null) {
                        previousField.requestFocusInWindow();
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && nextField != null) {
                        nextField.requestFocusInWindow();
                    }
                }
            });
        }

        void setNavigationTargets(JTextField previousField, JTextField nextField) {
            this.previousField = previousField;
            this.nextField = nextField;
        }
    }

    static class DigitField extends BaseNavField {
        private final int maxDigit;

        DigitField(int maxDigit) {
            super(1);
            this.maxDigit = maxDigit;
            setFont(new Font("Consolas", Font.BOLD, 22));
            setPreferredSize(new Dimension(62, 52));
            setMinimumSize(new Dimension(62, 52));
            setCaretColor(PRIMARY_DARK);
            ((AbstractDocument) getDocument()).setDocumentFilter(new DigitFilter());
        }

        private class DigitFilter extends DocumentFilter {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;

                String normalized = text.trim();
                if (normalized.isEmpty()) {
                    super.replace(fb, 0, fb.getDocument().getLength(), "", attrs);
                    return;
                }

                char ch = normalized.charAt(normalized.length() - 1);
                if (ch < '1' || ch > (char) ('0' + maxDigit)) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                super.replace(fb, 0, fb.getDocument().getLength(), String.valueOf(ch), attrs);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (nextField != null) {
                            nextField.requestFocusInWindow();
                            nextField.selectAll();
                        }
                    }
                });
            }
        }
    }

    static class OXField extends BaseNavField {
        OXField() {
            super(1);
            setFont(new Font("Consolas", Font.BOLD, 18));
            setPreferredSize(new Dimension(52, 42));
            ((AbstractDocument) getDocument()).setDocumentFilter(new OXFilter());
        }

        private class OXFilter extends DocumentFilter {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                String normalized = text.trim().toUpperCase(Locale.ROOT);
                if (normalized.isEmpty()) {
                    super.replace(fb, 0, fb.getDocument().getLength(), "", attrs);
                    return;
                }
                char ch = normalized.charAt(normalized.length() - 1);
                if (ch != 'O' && ch != 'X') {
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
        }
    }

    static class SimpleJsonParser {
        private final String text;
        private int index;

        SimpleJsonParser(String text) { this.text = text; }

        Object parse() throws IOException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) throw new IOException("JSON 끝 이후에 불필요한 내용이 있습니다.");
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (index >= text.length()) throw new IOException("예상보다 빨리 JSON이 끝났습니다.");
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
            if (peek('}')) { expect('}'); return map; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) { expect('}'); break; }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() throws IOException {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) { expect(']'); return list; }
            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) { expect(']'); break; }
                expect(',');
            }
            return list;
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') return sb.toString();
                if (ch == '\\') {
                    if (index >= text.length()) throw new IOException("문자열 이스케이프가 잘못되었습니다.");
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
                            if (index + 4 > text.length()) throw new IOException("유니코드 이스케이프가 잘못되었습니다.");
                            String hex = text.substring(index, index + 4);
                            index += 4;
                            try { sb.append((char) Integer.parseInt(hex, 16)); }
                            catch (NumberFormatException e) { throw new IOException("유니코드 이스케이프가 잘못되었습니다: " + hex); }
                            break;
                        default: throw new IOException("지원하지 않는 이스케이프 문자: \\" + esc);
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
                if (number.contains(".")) return Double.parseDouble(number);
                return Long.parseLong(number);
            } catch (NumberFormatException e) {
                throw new IOException("숫자 형식이 잘못되었습니다: " + number);
            }
        }

        private boolean match(String keyword) {
            if (text.startsWith(keyword, index)) { index += keyword.length(); return true; }
            return false;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void expect(char expected) throws IOException {
            if (index >= text.length() || text.charAt(index) != expected) throw new IOException("'" + expected + "' 문자가 필요합니다.");
            index++;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        }
    }

    static void applyGroup(Object node, List<PartConfig> parts, Map<String, List<String>> target) throws IOException {
        if (!(node instanceof Map)) throw new IOException("파트 그룹 구조가 올바르지 않습니다.");
        @SuppressWarnings("unchecked") Map<String, Object> group = (Map<String, Object>) node;
        for (PartConfig part : parts) {
            Object partNode = group.get(part.name);
            if (!(partNode instanceof List)) throw new IOException(part.name + " 배열이 없습니다.");
            target.put(part.name, objectListToValidatedStringList((List<?>) partNode, part.count));
        }
    }

    static List<String> objectListToValidatedStringList(List<?> raw, int needLen) throws IOException {
        if (raw.size() != needLen) throw new IOException("문항 수가 맞지 않습니다. 필요=" + needLen + ", 실제=" + raw.size());
        List<String> result = new ArrayList<>();
        for (Object o : raw) {
            String v = String.valueOf(o).trim();
            if (!v.matches("[1-5]")) throw new IOException("정답 값은 1~5만 허용됩니다: " + v);
            result.add(v);
        }
        return result;
    }

    static String groupToJson(List<PartConfig> parts, Map<String, List<String>> answerMap, int indentSpaces) {
        String indent = " ".repeat(indentSpaces);
        String inner = " ".repeat(indentSpaces + 2);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (int i = 0; i < parts.size(); i++) {
            PartConfig part = parts.get(i);
            sb.append(inner).append("\"").append(part.name).append("\": ").append(stringListToJson(answerMap.get(part.name)));
            if (i < parts.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent).append("}");
        return sb.toString();
    }

    static String stringListToJson(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    static String intListToJson(List<Integer> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
