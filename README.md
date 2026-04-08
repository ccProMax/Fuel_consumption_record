# 油耗记录Android应用

一个简洁实用的Android油耗记录应用，帮助你追踪车辆的加油记录、计算油耗和统计费用。

## 功能特性

- **添加加油记录**：记录日期、里程数、加油量、单价、总价和备注
- **查看记录列表**：按时间倒序显示所有加油记录
- **自动计算油耗**：根据里程差自动计算每百公里油耗
- **统计信息卡片**：实时显示总花费、平均油耗、总里程和记录数
- **数据导出**：将记录导出为CSV文件，方便备份和分析
- **Material Design**：遵循Material Design设计规范，界面美观

## 技术栈

- **语言**: Kotlin
- **最小SDK**: API 21 (Android 5.0)
- **目标SDK**: API 34 (Android 14)
- **架构**: MVVM (Model-View-ViewModel)
- **数据库**: Room Persistence Library
- **UI组件**: Material Design Components, RecyclerView
- **异步处理**: Kotlin Coroutines

## 项目结构

```
油耗记录app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/fuelrecord/
│   │   │   ├── MainActivity.kt              # 主活动
│   │   │   ├── FuelRecordAdapter.kt         # 列表适配器
│   │   │   ├── model/
│   │   │   │   └── FuelRecord.kt            # 数据模型
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt           # Room数据库
│   │   │   │   └── FuelRecordDao.kt         # 数据访问对象
│   │   │   └── viewmodel/
│   │   │       └── FuelViewModel.kt         # 视图模型
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml        # 主界面布局
│   │   │   │   ├── item_fuel_record.xml     # 列表项布局
│   │   │   │   └── dialog_add_record.xml    # 添加记录对话框
│   │   │   ├── values/
│   │   │   │   ├── strings.xml              # 字符串资源
│   │   │   │   ├── colors.xml               # 颜色资源
│   │   │   │   └── styles.xml               # 样式资源
│   │   │   └── ...
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 快速开始

### 环境要求

1. **Android Studio**: Hedgehog (2023.1.1) 或更高版本
2. **JDK**: JDK 11 或更高版本
3. **Android SDK**: API 21+

### 运行步骤

1. 克隆或下载本项目
2. 用Android Studio打开项目根目录
3. 等待Gradle同步完成
4. 点击运行按钮，选择模拟器或真机运行

### 构建APK

```bash
# 在Android Studio终端或命令行执行
./gradlew assembleDebug
```

生成的APK位于 `app/build/outputs/apk/debug/app-debug.apk`

## 核心代码说明

### 数据模型 (FuelRecord.kt)

```kotlin
@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Date = Date(),           // 加油日期
    val mileage: Double,               // 当前里程
    val fuelAmount: Double,            // 加油量（升）
    val pricePerLiter: Double,         // 单价（元/升）
    val totalCost: Double = 0.0,       // 总价
    val note: String = "",             // 备注
    val fuelConsumption: Double = 0.0  // 油耗（L/100km）
)
```

### 油耗计算公式

```
油耗(L/100km) = (加油量 ÷ 行驶里程) × 100
其中：行驶里程 = 本次里程 - 上次里程
```

### 数据库操作 (FuelRecordDao.kt)

- 增删改查基本操作
- 统计查询：总花费、平均油耗、总里程等
- 支持日期范围查询和关键词搜索

### ViewModel (FuelViewModel.kt)

- 管理UI相关数据
- 提供LiveData供UI观察
- 处理业务逻辑和数据验证

## 截图预览

应用采用Material Design设计，包含：
- 顶部统计信息卡片
- 中间记录列表
- 右下角浮动添加按钮

## 待办事项

- [ ] 添加图表展示油耗趋势
- [ ] 支持多车辆管理
- [ ] 添加油费预算提醒
- [ ] 支持云同步
- [ ] 添加Widget桌面小组件

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！
