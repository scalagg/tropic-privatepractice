package gg.tropic.practice.command

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.tropic.practice.PracticeDevTools
import gg.tropic.practice.editor.Editor
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 7/18/2024
 */
@AutoRegister
@CommandPermission("op")
@CommandAlias("editor")
object EditorCommand : ScalaCommand()
{
    @Inject
    lateinit var plugin: PracticeDevTools

    @Default
    fun default(player: ScalaPlayer)
    {
        if (Editor.instances[player.uniqueId] != null)
        {
            throw ConditionFailedException("Already in editor!")
        }

        player.sendMessage("${CC.GOLD}Entering the editor...")
        Editor(player.bukkit()).apply {
            bindWith(plugin)
        }.initialize()
    }

    @Subcommand("leave")
    fun leave(player: ScalaPlayer)
    {
        if (Editor.instances[player.uniqueId] == null)
        {
            throw ConditionFailedException("Not in editor!")
        }

        Editor.instances[player.uniqueId]?.closeAndReportException()
        Editor.instances.remove(player.uniqueId)

        player.sendMessage("${CC.RED}Left editor.")
    }
}
