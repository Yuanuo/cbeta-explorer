package org.appxi.cbeta.app;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;
import org.appxi.cbeta.BookMap;
import org.appxi.cbeta.Bookcase;
import org.appxi.cbeta.BookcaseInZip;
import org.appxi.cbeta.TripitakaMap;
import org.appxi.cbeta.app.dao.DaoHelper;
import org.appxi.cbeta.app.dao.DaoService;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.DefaultOptions;
import org.appxi.javafx.settings.OptionEditorBase;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInMemory;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.smartcn.pinyin.PinyinHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class AppContext {
    public static final DataFormat DND_ITEM = new DataFormat("application/x-item-serialized-object");

    private static Bookcase bookcase;
    private static BookMap bookMap;

    static void setupBookcase(Bookcase bookcase) {
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
            PinyinHelper.pinyin("init");
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
                        if ("classpath*:org/appxi/cbeta/app/dao/**/*.class".equals(locationPattern)) {
                            URL url = AppContext.class.getResource("/org/appxi/cbeta/app/dao/PiecesRepository.class");
                            return null == url ? new Resource[0] : new Resource[]{new UrlResource(url)};
                        }
                        return new Resource[0];
                    }
                };
                App.app().eventBus.fireEvent(new GenericEvent(GenericEvent.BEANS_READY));
                App.app().logger.info(StringHelper.concat("beans init after: ",
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
        app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED, event -> hanLang = event.data());
        //
        SettingsList.add(() -> {
            final ObjectProperty<HanLang> valueProperty = new SimpleObjectProperty<>(AppContext.hanLang());
            valueProperty.addListener((o, ov, nv) -> {
                if (null == ov || Objects.equals(ov, nv)) return;
                //
                UserPrefs.prefs.setProperty("display.han", nv.lang);
                app.eventBus.fireEvent(new GenericEvent(GenericEvent.HAN_LANG_CHANGED, nv));
            });
            return new DefaultOptions<HanLang>("简繁体", "以 简体/繁体 显示经名标题、阅读视图等经藏数据", "VIEWER", true)
                    .setValues(HanLang.hans, HanLang.hant, HanLang.hantHK, HanLang.hantTW)
                    .setValueProperty(valueProperty);
        });
        //
        SettingsList.add(() -> {
            Preferences prefsG = new PreferencesInProperties(
                    Path.of(System.getProperty("user.home")).resolve(AppLauncher.dataDirName + "/.config/.prefs"));
            return new DefaultOption<String>("切换数据源", null, "***数据源", true,
                    option -> new OptionEditorBase<>(option, new Button()) {
                        private StringProperty valueProperty;

                        @Override
                        public StringProperty valueProperty() {
                            if (this.valueProperty == null) {
                                this.valueProperty = new SimpleStringProperty();
                                getEditor().setText("选择数据包");
                                getEditor().setOnAction(evt -> {
                                    final FileChooser chooser = new FileChooser();
                                    chooser.setTitle("请选择CBETA Bookcase Zip数据包");
                                    chooser.getExtensionFilters().add(
                                            new FileChooser.ExtensionFilter("CBETA Bookcase Zip File", "cbeta.zip", "bookcase.zip", "bookcase_*.zip")
                                    );
                                    final File selected = chooser.showOpenDialog(App.app().getPrimaryStage());
                                    if (null == selected) {
                                        return;
                                    }
                                    try {
                                        String path = selected.getAbsolutePath();
                                        // 验证数据源有效性
                                        new BookcaseInZip(path);
                                        // 数据源有效，更新记录
                                        prefsG.setProperty("bookcase", path);
                                        prefsG.save();
                                        setValue(path);

                                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "已切换数据源，下次启动时生效！");
                                        alert.setHeaderText(null);
                                        alert.initOwner(app.getPrimaryStage());
                                        alert.show();
                                    } catch (Throwable ignore) {
                                        App.app().toastError("验证失败，请选择有效的数据源！");
                                    }
                                });
                            }
                            return this.valueProperty;
                        }

                        @Override
                        public void setValue(String value) {
                            getEditor().setTooltip(new Tooltip("当前数据源：" + value));
                        }
                    }).setValue(prefsG.getString("bookcase", ""));
        });
    }

    private static HanLang hanLang;

    public static HanLang hanLang() {
        if (null == hanLang)
            hanLang = HanLang.valueBy(UserPrefs.prefs.getString("display.han", HanLang.hantTW.lang));
        return hanLang;
    }

    public static String hanText(String text) {
        return null == text ? "" : ChineseConvertors.convert(text, HanLang.hantTW, hanLang());
    }

    public static Preferences recentBooks = new PreferencesInMemory();
}
