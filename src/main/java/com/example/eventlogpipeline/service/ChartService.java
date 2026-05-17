package com.example.eventlogpipeline.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private static final Font KR_REGULAR = koreanFont(12f);
    private static final Font KR_BOLD    = koreanFont(13f).deriveFont(Font.BOLD);
    private static final Font KR_TITLE   = koreanFont(18f).deriveFont(Font.BOLD);

    private static final Color BG        = new Color(248, 249, 250);
    private static final Color TITLE_CLR = new Color(44,  62,  80);
    private static final Color AXIS_CLR  = new Color(93,  109, 126);
    private static final Color TICK_CLR  = new Color(127, 140, 141);
    private static final Color GRID_CLR  = new Color(220, 220, 220);
    private static final Color ACCENT    = new Color(91,  155, 213);

    private static final Color[] PALETTE = {
        new Color(91,  155, 213),
        new Color(112, 173, 71),
        new Color(255, 192, 0),
        new Color(237, 125, 49),
        new Color(165, 165, 165),
        new Color(68,  114, 196),
    };

    private static Font koreanFont(float size) {
        for (String name : new String[]{"Malgun Gothic", "Noto Sans CJK KR", "NanumGothic", "AppleGothic"}) {
            Font f = new Font(name, Font.PLAIN, (int) size);
            if (!f.getFamily().equals("Dialog")) return f.deriveFont(size);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }

    private final JdbcTemplate jdbcTemplate;

    @Value("${chart.output-dir:images/charts}")
    private String outputDir;

    public void generateAll() {
        new File(outputDir).mkdirs();

        generateEventTypeChart();
        generateSuccessFailChart();
        generateHourlyEventChart();
        generateDeviceOrderRateChart();
        generateCategorySalesChart();
        generateProductRevenueChart();
        generateDiscountRangeChart();
        generateDiscountRateChart();
        generatePriceRangeChart();
        generateProductFailRateChart();
        generateGradeDeviceChart();

        log.info("=== 차트 생성 완료: {} ===", new File(outputDir).getAbsolutePath());
    }

    // ── 기존 차트 ──────────────────────────────────

    private void generateEventTypeChart() {
        String sql = "SELECT event_type, COUNT(*) AS count FROM event_logs GROUP BY event_type ORDER BY count DESC";
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("count")).longValue(), "건수", (String) r.get("event_type")));

        save(barChart("이벤트 타입별 발생 횟수", "이벤트 타입", "건수", ds, true), "event_type_count.png", 900, 520);
    }

    private void generateSuccessFailChart() {
        String sql = "SELECT CASE WHEN is_succeeded THEN '정상' ELSE '실패' END AS status, COUNT(*) AS count FROM event_logs GROUP BY is_succeeded";
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.setValue((String) r.get("status"), ((Number) r.get("count")).longValue()));

        save(donutChart("전체 이벤트 중 성공과 실패 비율", ds), "success_fail_ratio.png", 700, 520);
    }

    private void generateHourlyEventChart() {
        String sql = "SELECT EXTRACT(HOUR FROM event_time) AS hour, COUNT(*) AS total_events FROM event_logs GROUP BY hour ORDER BY hour";
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("total_events")).longValue(), "이벤트 수",
                        String.format("%02d시", ((Number) r.get("hour")).intValue())));

        save(barChart("시간대별 이벤트 발생량", "시간대", "이벤트 수", ds, false), "hourly_events.png", 1100, 520);
    }

    private void generateDeviceOrderRateChart() {
        String sql = """
            SELECT device_type,
                   COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END) AS order_count,
                   COUNT(*) AS total_events
            FROM event_logs WHERE device_type IS NOT NULL GROUP BY device_type
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r -> {
            long orders = ((Number) r.get("order_count")).longValue();
            long total  = ((Number) r.get("total_events")).longValue();
            double rate = total > 0 ? Math.round(orders * 10000.0 / total) / 100.0 : 0;
            ds.addValue(rate, "주문율(%)", (String) r.get("device_type"));
        });

        save(barChart("디바이스 타입별 주문율", "디바이스", "주문율 (%)", ds, true), "device_order_rate.png", 700, 520);
    }

    private void generateCategorySalesChart() {
        String sql = """
            SELECT c.category_name, SUM(o.quantity) AS total_quantity
            FROM orders o
              JOIN products p ON o.product_id = p.product_id
              JOIN categories c ON p.category_id = c.category_id
            GROUP BY c.category_id, c.category_name ORDER BY total_quantity DESC
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("total_quantity")).longValue(), "수량", (String) r.get("category_name")));

        save(barChart("카테고리별 판매량", "카테고리", "총 수량", ds, true), "category_sales.png", 900, 520);
    }

    // ── 신규 차트 ──────────────────────────────────

    private void generateProductRevenueChart() {
        String sql = """
            SELECT p.product_name, SUM(o.price_at_order * o.quantity) AS total_revenue
            FROM orders o JOIN products p ON o.product_id = p.product_id
            GROUP BY p.product_id, p.product_name ORDER BY total_revenue DESC
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("total_revenue")).doubleValue(), "매출", (String) r.get("product_name")));

        save(barChart("상품별 총 매출", "상품", "매출 (원)", ds, true), "product_revenue.png", 1000, 520);
    }

    private void generateDiscountRangeChart() {
        String sql = """
            SELECT CASE
                       WHEN discount_at_order IS NULL OR discount_at_order = 0 THEN '할인 없음'
                       WHEN discount_at_order < 0.1 THEN '10% 미만'
                       WHEN discount_at_order < 0.2 THEN '10~20%'
                       WHEN discount_at_order < 0.3 THEN '20~30%'
                       ELSE '30% 이상'
                   END AS discount_range,
                   SUM(quantity) AS total_quantity
            FROM orders
            GROUP BY discount_range
            ORDER BY MIN(CASE WHEN discount_at_order IS NULL OR discount_at_order = 0 THEN -1
                              ELSE discount_at_order END)
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("total_quantity")).longValue(), "구매량", (String) r.get("discount_range")));

        save(barChart("할인율 구간별 구매량", "할인율 구간", "구매량", ds, true), "discount_range_quantity.png", 900, 520);
    }

    private void generateDiscountRateChart() {
        String sql = """
            SELECT ROUND(COALESCE(discount_at_order, 0) * 100, 0)::int AS discount_percent,
                   SUM(quantity) AS total_quantity
            FROM orders
            GROUP BY discount_percent
            ORDER BY discount_percent
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("total_quantity")).longValue(), "구매량",
                        r.get("discount_percent") + "%"));

        save(barChart("할인율별 구매량", "할인율", "구매량", ds, false), "discount_rate_quantity.png", 900, 520);
    }

    private void generatePriceRangeChart() {
        String sql = """
            SELECT CASE
                       WHEN price_at_order < 50000   THEN '5만원 미만'
                       WHEN price_at_order < 200000  THEN '5~20만원'
                       WHEN price_at_order < 500000  THEN '20~50만원'
                       WHEN price_at_order < 1000000 THEN '50~100만원'
                       ELSE '100만원 이상'
                   END AS price_range,
                   SUM(quantity) AS total_quantity
            FROM orders
            GROUP BY price_range
            ORDER BY MIN(price_at_order)
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("total_quantity")).longValue(), "구매량", (String) r.get("price_range")));

        save(barChart("가격별 구매량", "가격대", "구매량", ds, true), "price_range_quantity.png", 900, 520);
    }

    private void generateProductFailRateChart() {
        String sql = """
            SELECT p.product_name,
                   ROUND(COUNT(CASE WHEN e.event_type = 'ORDER_FAILED' THEN 1 END)::numeric /
                         NULLIF(COUNT(CASE WHEN e.event_type IN ('ORDER_CREATED', 'ORDER_FAILED') THEN 1 END), 0) * 100, 1)
                         AS fail_rate_percent
            FROM event_logs e JOIN products p ON e.product_id = p.product_id
            WHERE e.event_type IN ('ORDER_CREATED', 'ORDER_FAILED')
            GROUP BY p.product_id, p.product_name
            ORDER BY fail_rate_percent DESC
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("fail_rate_percent")).doubleValue(), "실패율(%)", (String) r.get("product_name")));

        save(barChart("상품별 주문 실패율", "상품", "실패율 (%)", ds, true), "product_fail_rate.png", 1000, 520);
    }

    private void generateGradeDeviceChart() {
        String sql = """
            SELECT u.user_grade, e.device_type,
                   ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER (PARTITION BY u.user_grade) * 100, 1) AS ratio_percent
            FROM event_logs e JOIN users u ON e.user_id = u.user_id
            WHERE e.device_type IS NOT NULL
            GROUP BY u.user_grade, e.device_type
            ORDER BY u.user_grade, e.device_type
            """;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        jdbcTemplate.queryForList(sql).forEach(r ->
            ds.addValue(((Number) r.get("ratio_percent")).doubleValue(),
                        (String) r.get("device_type"),
                        (String) r.get("user_grade")));

        save(groupedBarChart("유저 등급별 디바이스 선호도", "유저 등급", "비율 (%)", ds), "grade_device.png", 900, 520);
    }

    // ── 차트 빌더 ─────────────────────────────────

    private JFreeChart barChart(String title, String xLabel, String yLabel,
                                DefaultCategoryDataset ds, boolean multiColor) {
        int cols = ds.getColumnCount();

        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int col) {
                return multiColor ? PALETTE[col % PALETTE.length] : ACCENT;
            }
        };
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(cols <= 6 ? 0.45 : 0.85);
        renderer.setItemMargin(0.08);

        JFreeChart chart = buildCategoryChart(title, xLabel, yLabel, ds, renderer, false);
        if (cols > 8) {
            chart.getCategoryPlot().getDomainAxis()
                 .setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }
        return chart;
    }

    private JFreeChart lineChart(String title, String xLabel, String yLabel, DefaultCategoryDataset ds) {
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, ACCENT);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesFillPaint(0, Color.WHITE);
        renderer.setUseFillPaint(true);

        JFreeChart chart = buildCategoryChart(title, xLabel, yLabel, ds, renderer, false);
        chart.getCategoryPlot().getDomainAxis()
             .setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        return chart;
    }

    private JFreeChart groupedBarChart(String title, String xLabel, String yLabel, DefaultCategoryDataset ds) {
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int col) {
                return PALETTE[row % PALETTE.length];
            }
        };
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.35);
        renderer.setItemMargin(0.05);

        return buildCategoryChart(title, xLabel, yLabel, ds, renderer, true);
    }

    private JFreeChart buildCategoryChart(String title, String xLabel, String yLabel,
                                          DefaultCategoryDataset ds,
                                          AbstractCategoryItemRenderer renderer,
                                          boolean showLegend) {
        CategoryAxis domainAxis = new CategoryAxis(xLabel);
        domainAxis.setLabelFont(KR_BOLD);
        domainAxis.setLabelPaint(AXIS_CLR);
        domainAxis.setTickLabelFont(KR_REGULAR);
        domainAxis.setTickLabelPaint(TICK_CLR);
        domainAxis.setAxisLineVisible(false);
        domainAxis.setTickMarksVisible(false);
        domainAxis.setUpperMargin(0.02);
        domainAxis.setLowerMargin(0.02);

        NumberAxis rangeAxis = new NumberAxis(yLabel);
        rangeAxis.setLabelFont(KR_BOLD);
        rangeAxis.setLabelPaint(AXIS_CLR);
        rangeAxis.setTickLabelFont(KR_REGULAR);
        rangeAxis.setTickLabelPaint(TICK_CLR);
        rangeAxis.setAxisLineVisible(false);
        rangeAxis.setTickMarksVisible(false);
        rangeAxis.setAutoRangeIncludesZero(true);

        CategoryPlot plot = new CategoryPlot(ds, domainAxis, rangeAxis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(GRID_CLR);
        plot.setRangeGridlineStroke(new BasicStroke(0.8f));
        plot.setInsets(new RectangleInsets(15, 5, 5, 15));
        plot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, showLegend);
        chart.setBackgroundPaint(BG);
        chart.setPadding(new RectangleInsets(20, 20, 15, 20));

        TextTitle textTitle = new TextTitle(title, KR_TITLE);
        textTitle.setPaint(TITLE_CLR);
        textTitle.setPadding(new RectangleInsets(5, 0, 15, 0));
        chart.setTitle(textTitle);

        if (showLegend && chart.getLegend() != null) {
            chart.getLegend().setItemFont(KR_REGULAR);
            chart.getLegend().setItemPaint(AXIS_CLR);
            chart.getLegend().setBackgroundPaint(Color.WHITE);
            chart.getLegend().setBorder(0, 0, 0, 0);
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        }

        return chart;
    }

    private JFreeChart donutChart(String title, DefaultPieDataset<String> ds) {
        RingPlot plot = new RingPlot(ds);
        plot.setSectionDepth(0.38);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(KR_REGULAR);
        plot.setLabelPaint(TITLE_CLR);
        plot.setLabelBackgroundPaint(null);
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelGap(0.02);
        plot.setSectionPaint("정상", new Color(91,  155, 213));
        plot.setSectionPaint("실패", new Color(237, 125, 49));
        plot.setInsets(new RectangleInsets(10, 10, 10, 10));

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(BG);
        chart.setPadding(new RectangleInsets(20, 20, 15, 20));

        TextTitle textTitle = new TextTitle(title, KR_TITLE);
        textTitle.setPaint(TITLE_CLR);
        textTitle.setPadding(new RectangleInsets(5, 0, 15, 0));
        chart.setTitle(textTitle);

        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(KR_REGULAR);
            chart.getLegend().setItemPaint(AXIS_CLR);
            chart.getLegend().setBackgroundPaint(Color.WHITE);
            chart.getLegend().setBorder(0, 0, 0, 0);
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        }

        return chart;
    }

    private void save(JFreeChart chart, String filename, int width, int height) {
        File file = new File(outputDir, filename);
        try {
            ChartUtils.saveChartAsPNG(file, chart, width, height);
            log.info("차트 저장: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("차트 저장 실패: {}", filename, e);
        }
    }
}