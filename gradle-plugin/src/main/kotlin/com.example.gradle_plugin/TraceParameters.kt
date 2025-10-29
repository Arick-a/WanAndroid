// TraceParameters.kt (新增)
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface TraceParameters : InstrumentationParameters {
    @get:Input
    val open: Property<Boolean>

    @get:Input
    val logTraceInfo: Property<Boolean>

    // 1. 传递 Beat Class 名称
    @get:Input
    val beatClass: Property<String>

    // 2. 传递需要插桩的包列表
    @get:Input
    val needTracePackages: ListProperty<String>

    // 3. 传递白名单包列表
    @get:Input
    val whitePackages: ListProperty<String>

    // 4. 传递白名单类列表
    @get:Input
    val whiteClasses: ListProperty<String>

    @get:Input
    val traceConfigFile: Property<String>
}