package build

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputFile
import java.io.File

open class TaskSpec(
    var taskName: String = ""
)

class DistributionPackageSettings(val dir: String, val fileName: String)

class UploadTaskSpecs <T : TaskSpec>(
    val packageSettings: DistributionPackageSettings,
    private val repoName: String,
    private val taskGroup: String,
    val stable: T,
    val dev: T
) {
    init {
        this.stable.taskName = taskName("Stable")
        this.dev.taskName = taskName("Dev")
    }

    private fun taskName(type: String) = repoName + "Upload" + type

    fun createTasks(options: KernelBuildExtension, taskCreationAction: (T) -> Unit) {
        val project = options.project
        if (options.isOnProtectedBranch) {
            taskCreationAction(stable)
        }
        taskCreationAction(dev)

        project.task(taskName("Protected")) {
            dependsOn(project.tasks.getByName(CLEAN_INSTALL_DIR_DISTRIB_TASK))
            group = taskGroup
            if (options.isOnProtectedBranch) {
                dependsOn(dev.taskName)
            }
        }
    }
}

class CondaCredentials(
    val username: String,
    val password: String
)

class CondaTaskSpec(
    val username: String,
    val credentials: CondaCredentials
) : TaskSpec()

class PyPiTaskSpec(
    val repoURL: String,
    val username: String,
    val password: String
) : TaskSpec()

abstract class PipInstallReq : Exec() {
    @get:InputFile
    var requirementsFile: File? = null
        set(value) {
            commandLine("python", "-m", "pip", "install", "-r", value)
            field = value
        }
}
