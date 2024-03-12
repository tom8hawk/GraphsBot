package ru.tom8hawk.personal;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieSeries;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.colors.BaseSeriesColors;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Graphs {
    private static final Configuration configuration = Configuration.init();
    private static final TelegramBot bot = new TelegramBot(configuration.getToken());
    private static final Logger logger = Logger.getLogger(Graphs.class.getName());
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    private static byte[] EXAMPLE_CHART;
    private static final List<Color> COLORS = Arrays.asList(new BaseSeriesColors().getSeriesColors());

    public static void main(String[] args) {
        bot.setUpdatesListener(updates -> {
            updates.forEach(update ->
                    executor.execute(() -> processUpdate(update)));

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                BaseResponse response = e.response();
                logger.warning(() -> response.errorCode() + ": " + response.description());
            } else {
                logger.warning(e::getMessage);
            }
        });

        PieChart chart = createChart("Животные");
        chart.addSeries("Олени", 10);
        chart.addSeries("Слоны", 50);
        EXAMPLE_CHART = convertToByteArray(chart);
    }

    private static void processUpdate(Update update) {
        Message message = update.message();

        if (message != null) {
            String text = message.text();

            if (text != null && !text.isEmpty()) {
                long chatId = update.message().chat().id();

                if (text.equalsIgnoreCase("/start")) {
                    sendMessage(chatId, configuration.getStartMessage());
                    sendPhoto(chatId, EXAMPLE_CHART, configuration.getExampleRequestMessage());
                } else {
                    String[] lines = text.split("\n");

                    if (lines.length > 3 && lines[1].isEmpty()) {
                        String title = lines[0].trim();

                        if (!title.isEmpty()) {
                            PieChart chart = createChart(title);
                            Map<String, PieSeries> series = chart.getSeriesMap();

                            for (int i = 2; i < lines.length; i++) {
                                String line = lines[i].trim();

                                if (!line.isEmpty()) {
                                    String[] args = line.split(":");

                                    if (args.length == 2) {
                                        String key = args[0];
                                        float value;

                                        try {
                                            value = Float.parseFloat(args[1]);
                                        } catch (NumberFormatException ignored) {
                                            continue;
                                        }

                                        PieSeries existing = series.get(key);

                                        if (existing != null) {
                                            existing.setValue(existing.getValue().floatValue() + value);
                                        } else {
                                            series.put(key, new PieSeries(key, value));
                                        }
                                    }
                                }
                            }

                            List<Color> colors = new ArrayList<>(COLORS);
                            Collections.shuffle(colors);
                            chart.getStyler().setSeriesColors(colors.toArray(Color[]::new));

                            sendPhoto(chatId, convertToByteArray(chart), null);
                        }
                    }
                }
            }
        }
    }

    private static PieChart createChart(String title) {
        PieChart chart = new org.knowm.xchart.PieChartBuilder()
                .title(title)
                .width(500)
                .height(500)
                .build();

        PieStyler styler = chart.getStyler();

        Font helvetica = new Font("Helvetica", Font.PLAIN, 15);
        styler.setChartTitleFont(helvetica);
        styler.setChartTitlePadding(9);

        styler.setLegendVisible(true);
        styler.setLegendPosition(Styler.LegendPosition.OutsideE);
        styler.setLegendLayout(Styler.LegendLayout.Vertical);
        styler.setLegendPadding(4);
        styler.setLegendBorderColor(Color.WHITE);

        styler.setDefaultSeriesRenderStyle(PieSeries.PieSeriesRenderStyle.Donut);
        styler.setDonutThickness(.2);
        styler.setChartPadding(0);
        styler.setCircular(true);

        styler.setPlotContentSize(.9);
        styler.setPlotBorderColor(Color.WHITE);
        styler.setChartBackgroundColor(Color.WHITE);

        styler.setSumVisible(true);
        styler.setSumFontSize(20f);
        styler.setDecimalPattern("#");

        return chart;
    }

    private static byte[] convertToByteArray(PieChart chart) {
        try {
            return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SendResponse sendMessage(long chatId, String message) {
        return bot.execute(new SendMessage(chatId, message).parseMode(ParseMode.Markdown));
    }

    private static SendResponse sendPhoto(long chatId, byte[] photo, String caption) {
        SendPhoto sendPhoto = new SendPhoto(chatId, photo).parseMode(ParseMode.Markdown);

        if (caption != null) {
            sendPhoto.caption(caption);
        }

        return bot.execute(sendPhoto);
    }
}