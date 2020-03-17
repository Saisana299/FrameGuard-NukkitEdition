package saisana299.frameguard

import cn.nukkit.plugin.PluginBase
import cn.nukkit.Player
import cn.nukkit.IPlayer
import cn.nukkit.Server
import cn.nukkit.network.protocol.ItemFrameDropItemPacket
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.event.EventHandler
import cn.nukkit.event.Listener
import cn.nukkit.event.player.PlayerInteractEvent
import cn.nukkit.event.block.BlockBreakEvent
import cn.nukkit.event.server.DataPacketReceiveEvent
import cn.nukkit.math.BlockFace
import cn.nukkit.utils.Config

import java.util.UUID

class FrameGuard : PluginBase(), Listener {

    private lateinit var frames: Config

    private var frame: MutableMap<String, MutableMap<String, String>> = mutableMapOf("" to mutableMapOf("" to ""))

    override fun onEnable() {
        server.pluginManager.registerEvents(this,this)
        logger.info("§a額縁保護プラグインを読み込みました")
        if (!dataFolder.exists()) 
            dataFolder.mkdirs()
        saveResource("frames.yml")
        frames = Config("$dataFolder/frames.yml", Config.YAML)
    }

    override fun onDisable(){
        frames.save()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean{
        if(sender !is Player){
            sender.sendMessage("[FrameGuard] §cゲーム内で実行してください")
            return true
        }
        when(label){
            "fg" -> {
                val uuid = sender.uniqueId.toString()
                if(frame[uuid] == null){
                    frame[uuid] = mutableMapOf("type" to "add")
                    sender.sendMessage("[FrameGuard] 保護モードを有効にしました\n額縁をタップしてください\n再度コマンドを使うと保護モードを無効にできます")
                }else{
                    frame.remove(uuid)
                    sender.sendMessage("[FrameGuard] 保護・保護解除モードを無効にしました")
                }
            }
            "unfg" -> {
                val uuid = sender.uniqueId.toString()
                if(frame[uuid] == null){
                    frame[uuid] = mutableMapOf("type" to "delete")
                    sender.sendMessage("[FrameGuard] 保護解除モードを有効にしました\n額縁をタップしてください\n再度コマンドを使うと保護解除モードを無効にできます")
                }else{
                    frame.remove(uuid)
                    sender.sendMessage("[FrameGuard] 保護・保護解除モードを無効にしました")
                }
            }
        }
        return true
    }

    @EventHandler
    fun onReceived(event: DataPacketReceiveEvent){
        val packet = event.packet
        if(packet is ItemFrameDropItemPacket) {
            val player = event.player
            val uuid = player.uniqueId.toString()
            val xyz = "${packet.x},${packet.y},${packet.z},${player.getLevel().folderName}"
            if(frames.exists(xyz)){
                val owner = frames.get(xyz)
                if(!player.isOp){
                    if(owner != uuid){
                        val name: String = getName(owner as String)
                        player.sendMessage("[FrameGuard] §eこの額縁は §o§f${name}§r§e によって保護されています")
                        event.setCancelled()
                    }
                }else{
                    val name: String = getName(owner as String)
                    player.sendMessage("[FrameGuard] §eこの額縁は §o§f${name}§r§e によって保護されています")
                }
            }
        }
    }

    @EventHandler
    fun onTouch(event: PlayerInteractEvent){
        val player = event.player
        val block = event.block
        val uuid = player.uniqueId.toString()
        if(frame[uuid] != null){
            when(frame[uuid]?.get("type")){
                "add" -> {
                    if(block.id != 199){
                        player.sendMessage("[FrameGuard] §c額縁をタップしてください")
                        return
                    }
                    event.setCancelled()
                    val xyz = "${block.x.toInt()},${block.y.toInt()},${block.z.toInt()},${block.level.folderName}"
                    if(!frames.exists(xyz)){
                        frames.set(xyz, uuid)
                        player.sendMessage("[FrameGuard] §a額縁を保護しました")
                    }else{
                        player.sendMessage("[FrameGuard] §c既に保護されています")
                    }
                }
                "delete" -> {
                    if(block.id != 199){
                        player.sendMessage("[FrameGuard] §c額縁をタップしてください")
                        return
                    }
                    val place = "${block.x.toInt()},${block.y.toInt()},${block.z.toInt()},${block.level.folderName}"
                    if(frames.exists(place)){
                        val owner = frames.get(place)
                        if(owner == uuid){
                            frames.remove(place)
                            player.sendMessage("[FrameGuard] §a保護を解除しました")
                        }else{
                            if(!player.isOp){
                                val name: String = getName(owner as String)
                                player.sendMessage("[FrameGuard] §eこの額縁は §o§f${name}§r§e によって保護されています")
                            }else{
                                frames.remove(place)
                                player.sendMessage("[FrameGuard] §a保護を解除しました")
                            }
                        }
                    }else{
                        player.sendMessage("[FrameGuard] §cこの額縁は保護されていません")
                    }
                }
            }
        }
    }

    @EventHandler
    fun onBreakEvent(event: BlockBreakEvent){
        val player = event.player
        val uuid = player.uniqueId.toString()
        val block = event.block
        val xyz = "${block.x.toInt()},${block.y.toInt()},${block.z.toInt()},${block.level.folderName}"
        if(frames.exists(xyz)){
            val owner = frames.get(xyz)
            if(!player.isOp){
                if(owner != uuid){
                    val name: String = getName(owner as String)
                    player.sendMessage("[FrameGuard] §eこの額縁は §o§f${name}§r§e によって保護されています")
                    event.setCancelled()
                }else{
                    frames.remove(xyz)
                    player.sendMessage("[FrameGuard] §a額縁の保護を解除しました")
                }
            }else{
                frames.remove(xyz)
                player.sendMessage("[FrameGuard] §a額縁の保護を解除しました")
            }
        }else{
            val sides = listOf(BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH)
            sides.forEach{ element ->
                if(block.getSide(element).id == 199){
                    if(block.getSide(element).damage == 0){
                        val newSide = block.getSide(element)
                        val newBlock = newSide.getSide(BlockFace.WEST)
                        if("${newBlock.x.toInt()} ${newBlock.y.toInt()} ${newBlock.z.toInt()}" != "${block.x.toInt()} ${block.y.toInt()} ${block.z.toInt()}"){
                            return
                        }
                    }else if(block.getSide(element).damage == 1){
                        val newSide = block.getSide(element)
                        val newBlock = newSide.getSide(BlockFace.EAST)
                        if("${newBlock.x.toInt()} ${newBlock.y.toInt()} ${newBlock.z.toInt()}" != "${block.x.toInt()} ${block.y.toInt()} ${block.z.toInt()}"){
                            return
                        }
                    }else if(block.getSide(element).damage == 2){
                        val newSide = block.getSide(element)
                        val newBlock = newSide.getSide(BlockFace.NORTH)
                        if("${newBlock.x.toInt()} ${newBlock.y.toInt()} ${newBlock.z.toInt()}" != "${block.x.toInt()} ${block.y.toInt()} ${block.z.toInt()}"){
                            return
                        }
                    }else if(block.getSide(element).damage == 3){
                        val newSide = block.getSide(element)
                        val newBlock = newSide.getSide(BlockFace.SOUTH)
                        if("${newBlock.x.toInt()} ${newBlock.y.toInt()} ${newBlock.z.toInt()}" != "${block.x.toInt()} ${block.y.toInt()} ${block.z.toInt()}"){
                            return
                        }
                    }
                    val newPos = "${block.getSide(element).x.toInt()},${block.getSide(element).y.toInt()},${block.getSide(element).z.toInt()},${block.level.folderName}"
                    if(frames.exists(newPos)){
                        val owner = frames.get(newPos)
                        if(!player.isOp){
                            if(owner != uuid){
                                val name: String = getName(owner as String)
                                player.sendMessage("[FrameGuard] §o§f${name}§r§e によって保護されている額縁があるため壊せません")
                                event.setCancelled()
                            }else{
                                frames.remove(newPos)
                                player.sendMessage("[FrameGuard] §aブロック破壊で外れた額縁の保護を解除しました")
                            }
                        }else{
                            frames.remove(newPos)
                            player.sendMessage("[FrameGuard] §aブロック破壊で外れた額縁の保護を解除しました")
                        }
                    }
                }
            }
        }
    }

    private fun getName(stringUuid: String): String {
        val newUuid: UUID
        try {
            newUuid = UUID.fromString(stringUuid)
        } catch (e: Exception) {
            return "§7[UUID: $stringUuid]§r"
        }

        val iPlayer: IPlayer = Server.getInstance().getOfflinePlayer(newUuid)
        if (iPlayer.name != null) {
            return iPlayer.name
        }
        return "§7[UUID: $stringUuid]§r"
    }
}