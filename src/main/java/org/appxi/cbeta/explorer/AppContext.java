package org.appxi.cbeta.explorer;

import appxi.cbeta.BookMap;
import appxi.cbeta.Bookcase;
import appxi.cbeta.TripitakaMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.appxi.cbeta.explorer.dao.DaoHelper;
import org.appxi.cbeta.explorer.dao.DaoService;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.javafx.settings.DefaultOptions;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInMemory;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.smartcn.pinyin.Pinyin;
import org.appxi.smartcn.pinyin.PinyinConvertors;
import org.appxi.timeago.TimeAgo;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AppContext {
    private static Bookcase bookcase;
    private static BookMap bookMap;

    static void setupInitialize(Bookcase bookcase) {
        try {
            // 尝试优先使用绿色版数据
            final Path bookcaseDir = Path.of(bookcase.getPath()).resolve("../".concat(AppLauncher.dataDirName));
            if (Files.exists(bookcaseDir) && Files.isDirectory(bookcaseDir) && Files.isWritable(bookcaseDir)) {
                //
                UserPrefs.prefs.save();
                // reset
                UserPrefs.setupDataDirectory(bookcaseDir, null);
                UserPrefs.prefs = new PreferencesInProperties(UserPrefs.confDir().resolve(".prefs"));
            }
        } catch (Throwable ignore) {
        }
        //
        AppContext.bookcase = AppContext.bookcase == null ? bookcase : AppContext.bookcase;
        AppContext.bookMap = bookMap == null ? new BookMap(new TripitakaMap(AppContext.bookcase)) : bookMap;
//        // 如果以上执行出错，程序也尚未初始化完成，只有在基础数据正常时，再加载更多数据

        // 在此设置数据库基本环境，以供后续的功能正常使用
        DaoHelper.setupDatabaseService(UserPrefs.dataDir().resolve(".db"));
        new Thread(() -> {
            AppContext.booksMap().data();
            DaoService.setupInitialize();
            ChineseConvertors.hans2HantTW("测试");
            AppContext.ascii("init");
        }).start();
    }

    public static Bookcase bookcase() {
        return bookcase;
    }

    public static BookMap booksMap() {
        return bookMap;
    }

    private static AnnotationConfigApplicationContext beans;
    private static final Object _initBeans = new Object();

    public static BeanFactory beans() {
        if (null != beans)
            return beans;
        synchronized (_initBeans) {
            if (null != beans)
                return beans;
            try {
                beans = new AnnotationConfigApplicationContext(SpringConfig.class) {
                    @Override
                    public Resource[] getResources(String locationPattern) {
                        if ("classpath*:org/appxi/cbeta/explorer/dao/**/*.class".equals(locationPattern)) {
                            URL url = AppContext.class.getResource("/org/appxi/cbeta/explorer/dao/PiecesRepository.class");
                            return null == url ? new Resource[0] : new Resource[]{new UrlResource(url)};
                        }
                        return new Resource[0];
                    }
                };
                App.app().eventBus.fireEvent(new GenericEvent(GenericEvent.BEANS_READY));
                App.app().logger.warn(StringHelper.concat("beans init after: ",
                        System.currentTimeMillis() - App.app().startTime));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return beans;
    }

    public static <T> T getBean(Class<T> requiredType) {
        try {
            return beans().getBean(requiredType);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private AppContext() {
    }

    static void setupInitialize(App app) {
        app.eventBus.addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED, event -> displayHan = event.data());
        //
        SettingsList.add(() -> {
            final ObjectProperty<HanLang> valueProperty = new SimpleObjectProperty<>(AppContext.getDisplayHan());
            valueProperty.addListener((o, ov, nv) -> {
                if (null == ov || Objects.equals(ov, nv)) return;
                //
                UserPrefs.prefs.setProperty("display.han", nv.lang);
                app.eventBus.fireEvent(new GenericEvent(GenericEvent.DISPLAY_HAN_CHANGED, nv));
            });
            return new DefaultOptions<HanLang>("简繁体", "以 简体/繁体 显示经名标题、阅读视图等经藏数据", "VIEWER", true)
                    .setValues(HanLang.hans, HanLang.hant, HanLang.hantHK, HanLang.hantTW)
                    .setValueProperty(valueProperty);
        });
    }

    private static HanLang displayHan;

    public static HanLang getDisplayHan() {
        if (null == displayHan)
            displayHan = HanLang.valueBy(UserPrefs.prefs.getString("display.han", HanLang.hantTW.lang));
        return displayHan;
    }

    public static String displayText(String text) {
        return null == text ? "" : ChineseConvertors.convert(text, HanLang.hantTW, getDisplayHan());
    }

    private static TimeAgo.Messages timeAgoI18N;
    private static final Object _initTimeAgoI18N = new Object();

    public static TimeAgo.Messages timeAgoI18N() {
        if (null == timeAgoI18N)
            synchronized (_initTimeAgoI18N) {
                if (null == timeAgoI18N)
                    timeAgoI18N = TimeAgo.MessagesBuilder.start().withLocale("zh").build();
            }
        return timeAgoI18N;
    }

    public static String ascii(String text) {
        return ascii(text, false);
    }

    public static String ascii(String text, boolean tone) {
        final List<Map.Entry<Character, Pinyin>> pinyinList = null == text ? List.of() : PinyinConvertors.convert(text);
        StringBuilder result = new StringBuilder(pinyinList.size() * (6));
        pinyinList.forEach(entry -> {
            if (null == entry.getValue()) result.append(entry.getKey());
            else result.append(" ").append(tone ? entry.getValue().getPinyinWithToneMark() : entry.getValue().getPinyinWithoutTone()).append(" ");
        });

        // 原始数据中的空格有多有少，此处需要保证仅有1个空格，以方便匹配用户输入的数据
        return result.toString().replaceAll("\s+", " ").strip();
    }

    public static Preferences recentBooks = new PreferencesInMemory();
}
