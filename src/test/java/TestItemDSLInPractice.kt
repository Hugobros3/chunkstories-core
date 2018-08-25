import io.xol.chunkstories.api.dsl.ItemDeclarationsContext
import org.junit.Test

class TestItemDSLInPractice {

    var ctx: ItemDeclarationsContext? = null

    @Test
    fun omg() {
        ctx?.apply {
            item(io.xol.chunkstories.api.item.ItemVoxel::class) {
                name = "item_voxel"
                maxStackSize = 100
            }

            /** deprecated */
            item(io.xol.chunkstories.api.item.ItemVoxel::class) {
                name = "item_voxel_1x2"

                maxStackSize = 100
                slotsHeight = 2
            }

            item(io.xol.chunkstories.core.item.ItemMiningTool::class) {
                name = "iron_pickaxe"

                prototype {
                    toolType = "pickaxe"
                    miningEfficiency = 1.0f
                }

                representation {
                    model("./items/models/pickaxe.dae")
                }
            }

            item(io.xol.chunkstories.core.item.ItemMiningTool::class) {
                name = "iron_shovel"

                prototype {
                    toolType = "shovel"
                    miningEfficiency = 1.0f
                }
            }

            item(io.xol.chunkstories.core.item.ItemMiningTool::class) {
                name = "iron_axe"

                prototype {
                    toolType = "axe"
                    miningEfficiency = 1.0f
                }
            }

            item(io.xol.chunkstories.core.item.ItemFood::class) {
                name = "bread"

                maxStackSize = 5

                prototype {
                    calories = 20f
                }

                ext["mdr"] = ":D"
            }
        }
    }
}