package xyz.chunkstories.core.generator

import xyz.chunkstories.api.math.random.PrecomputedSimplexSeed
import xyz.chunkstories.api.math.random.SeededSimplexNoiseGenerator
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

fun main() {
    val ssng = SeededSimplexNoiseGenerator("1568906743471")
    val precomp = PrecomputedSimplexSeed("1568906743471")

    fun ridgedNoise(x: Float, z: Float, octaves: Int, freq: Float, persistence: Float): Float {
        var frequency = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f

        for (i in 0 until octaves) {
            total += (1.0f - abs(ssng.looped_noise(x * frequency, z * frequency, 1.0f))) * amplitude
            frequency *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }

        return total / maxAmplitude
    }

    val img = File("./heightmap.png")
    img.parentFile.mkdirs()
    img.delete()

    val imgBuf = BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB)
    for (x in 0..511) {
        for (y in 0..511) {
            var rgb = 0xff shl 24

            val xf = x / 512.0f
            val yf = y / 512.0f

            val v = ridgedNoise(xf, yf, 4, 1.0f, 0.5f)
            val vi = (v * 255.0f).toInt().coerceIn(0, 255)

            rgb = rgb or vi
            rgb = rgb or (vi shl 8)
            rgb = rgb or (vi shl 16)

            imgBuf.setRGB(x, y, rgb)
        }
    }

    ImageIO.write(imgBuf, "PNG", img)

    val list = ssng.perm.map { it.toInt() }

    val tada = """
perm[0],"49",int
perm[1],"54",int
perm[2],"56",int
perm[3],"59",int
perm[4],"61",int
perm[5],"48",int
perm[6],"58",int
perm[7],"62",int
perm[8],"55",int
perm[9],"53",int
perm[10],"60",int
perm[11],"66",int
perm[12],"51",int
perm[13],"52",int
perm[14],"67",int
perm[15],"69",int
perm[16],"72",int
perm[17],"74",int
perm[18],"50",int
perm[19],"71",int
perm[20],"75",int
perm[21],"68",int
perm[22],"65",int
perm[23],"73",int
perm[24],"79",int
perm[25],"63",int
perm[26],"64",int
perm[27],"80",int
perm[28],"82",int
perm[29],"85",int
perm[30],"87",int
perm[31],"57",int
perm[32],"84",int
perm[33],"88",int
perm[34],"81",int
perm[35],"78",int
perm[36],"86",int
perm[37],"92",int
perm[38],"76",int
perm[39],"77",int
perm[40],"93",int
perm[41],"95",int
perm[42],"98",int
perm[43],"100",int
perm[44],"70",int
perm[45],"97",int
perm[46],"101",int
perm[47],"94",int
perm[48],"91",int
perm[49],"99",int
perm[50],"105",int
perm[51],"89",int
perm[52],"90",int
perm[53],"106",int
perm[54],"108",int
perm[55],"111",int
perm[56],"113",int
perm[57],"83",int
perm[58],"110",int
perm[59],"114",int
perm[60],"107",int
perm[61],"104",int
perm[62],"112",int
perm[63],"118",int
perm[64],"102",int
perm[65],"103",int
perm[66],"119",int
perm[67],"121",int
perm[68],"124",int
perm[69],"126",int
perm[70],"96",int
perm[71],"123",int
perm[72],"127",int
perm[73],"120",int
perm[74],"117",int
perm[75],"125",int
perm[76],"131",int
perm[77],"115",int
perm[78],"116",int
perm[79],"132",int
perm[80],"134",int
perm[81],"137",int
perm[82],"139",int
perm[83],"109",int
perm[84],"136",int
perm[85],"140",int
perm[86],"133",int
perm[87],"130",int
perm[88],"138",int
perm[89],"144",int
perm[90],"128",int
perm[91],"129",int
perm[92],"145",int
perm[93],"147",int
perm[94],"150",int
perm[95],"152",int
perm[96],"122",int
perm[97],"149",int
perm[98],"153",int
perm[99],"146",int
perm[100],"143",int
perm[101],"151",int
perm[102],"157",int
perm[103],"141",int
perm[104],"142",int
perm[105],"158",int
perm[106],"160",int
perm[107],"163",int
perm[108],"165",int
perm[109],"135",int
perm[110],"162",int
perm[111],"166",int
perm[112],"159",int
perm[113],"156",int
perm[114],"164",int
perm[115],"170",int
perm[116],"154",int
perm[117],"155",int
perm[118],"171",int
perm[119],"173",int
perm[120],"176",int
perm[121],"178",int
perm[122],"148",int
perm[123],"175",int
perm[124],"179",int
perm[125],"172",int
perm[126],"169",int
perm[127],"177",int
perm[128],"183",int
perm[129],"167",int
perm[130],"168",int
perm[131],"184",int
perm[132],"186",int
perm[133],"189",int
perm[134],"191",int
perm[135],"161",int
perm[136],"188",int
perm[137],"192",int
perm[138],"185",int
perm[139],"182",int
perm[140],"190",int
perm[141],"196",int
perm[142],"180",int
perm[143],"181",int
perm[144],"197",int
perm[145],"199",int
perm[146],"202",int
perm[147],"204",int
perm[148],"174",int
perm[149],"201",int
perm[150],"205",int
perm[151],"198",int
perm[152],"195",int
perm[153],"203",int
perm[154],"209",int
perm[155],"193",int
perm[156],"194",int
perm[157],"210",int
perm[158],"212",int
perm[159],"215",int
perm[160],"217",int
perm[161],"187",int
perm[162],"214",int
perm[163],"218",int
perm[164],"211",int
perm[165],"208",int
perm[166],"216",int
perm[167],"222",int
perm[168],"206",int
perm[169],"207",int
perm[170],"223",int
perm[171],"225",int
perm[172],"228",int
perm[173],"230",int
perm[174],"200",int
perm[175],"227",int
perm[176],"231",int
perm[177],"224",int
perm[178],"221",int
perm[179],"229",int
perm[180],"235",int
perm[181],"219",int
perm[182],"220",int
perm[183],"236",int
perm[184],"238",int
perm[185],"241",int
perm[186],"243",int
perm[187],"213",int
perm[188],"240",int
perm[189],"244",int
perm[190],"237",int
perm[191],"234",int
perm[192],"242",int
perm[193],"248",int
perm[194],"232",int
perm[195],"233",int
perm[196],"249",int
perm[197],"251",int
perm[198],"254",int
perm[199],"0",int
perm[200],"239",int
perm[201],"255",int
perm[202],"1",int
perm[203],"253",int
perm[204],"252",int
perm[205],"2",int
perm[206],"3",int
perm[207],"4",int
perm[208],"5",int
perm[209],"6",int
perm[210],"7",int
perm[211],"8",int
perm[212],"9",int
perm[213],"10",int
perm[214],"11",int
perm[215],"12",int
perm[216],"13",int
perm[217],"14",int
perm[218],"15",int
perm[219],"16",int
perm[220],"17",int
perm[221],"18",int
perm[222],"19",int
perm[223],"20",int
perm[224],"21",int
perm[225],"22",int
perm[226],"23",int
perm[227],"24",int
perm[228],"25",int
perm[229],"26",int
perm[230],"27",int
perm[231],"28",int
perm[232],"29",int
perm[233],"30",int
perm[234],"31",int
perm[235],"32",int
perm[236],"33",int
perm[237],"34",int
perm[238],"35",int
perm[239],"36",int
perm[240],"37",int
perm[241],"38",int
perm[242],"39",int
perm[243],"40",int
perm[244],"41",int
perm[245],"42",int
perm[246],"43",int
perm[247],"44",int
perm[248],"45",int
perm[249],"46",int
perm[250],"47",int
perm[251],"226",int
perm[252],"245",int
perm[253],"246",int
perm[254],"247",int
perm[255],"250",int
perm[256],"49",int
perm[257],"54",int
perm[258],"56",int
perm[259],"59",int
perm[260],"61",int
perm[261],"48",int
perm[262],"58",int
perm[263],"62",int
perm[264],"55",int
perm[265],"53",int
perm[266],"60",int
perm[267],"66",int
perm[268],"51",int
perm[269],"52",int
perm[270],"67",int
perm[271],"69",int
perm[272],"72",int
perm[273],"74",int
perm[274],"50",int
perm[275],"71",int
perm[276],"75",int
perm[277],"68",int
perm[278],"65",int
perm[279],"73",int
perm[280],"79",int
perm[281],"63",int
perm[282],"64",int
perm[283],"80",int
perm[284],"82",int
perm[285],"85",int
perm[286],"87",int
perm[287],"57",int
perm[288],"84",int
perm[289],"88",int
perm[290],"81",int
perm[291],"78",int
perm[292],"86",int
perm[293],"92",int
perm[294],"76",int
perm[295],"77",int
perm[296],"93",int
perm[297],"95",int
perm[298],"98",int
perm[299],"100",int
perm[300],"70",int
perm[301],"97",int
perm[302],"101",int
perm[303],"94",int
perm[304],"91",int
perm[305],"99",int
perm[306],"105",int
perm[307],"89",int
perm[308],"90",int
perm[309],"106",int
perm[310],"108",int
perm[311],"111",int
perm[312],"113",int
perm[313],"83",int
perm[314],"110",int
perm[315],"114",int
perm[316],"107",int
perm[317],"104",int
perm[318],"112",int
perm[319],"118",int
perm[320],"102",int
perm[321],"103",int
perm[322],"119",int
perm[323],"121",int
perm[324],"124",int
perm[325],"126",int
perm[326],"96",int
perm[327],"123",int
perm[328],"127",int
perm[329],"120",int
perm[330],"117",int
perm[331],"125",int
perm[332],"131",int
perm[333],"115",int
perm[334],"116",int
perm[335],"132",int
perm[336],"134",int
perm[337],"137",int
perm[338],"139",int
perm[339],"109",int
perm[340],"136",int
perm[341],"140",int
perm[342],"133",int
perm[343],"130",int
perm[344],"138",int
perm[345],"144",int
perm[346],"128",int
perm[347],"129",int
perm[348],"145",int
perm[349],"147",int
perm[350],"150",int
perm[351],"152",int
perm[352],"122",int
perm[353],"149",int
perm[354],"153",int
perm[355],"146",int
perm[356],"143",int
perm[357],"151",int
perm[358],"157",int
perm[359],"141",int
perm[360],"142",int
perm[361],"158",int
perm[362],"160",int
perm[363],"163",int
perm[364],"165",int
perm[365],"135",int
perm[366],"162",int
perm[367],"166",int
perm[368],"159",int
perm[369],"156",int
perm[370],"164",int
perm[371],"170",int
perm[372],"154",int
perm[373],"155",int
perm[374],"171",int
perm[375],"173",int
perm[376],"176",int
perm[377],"178",int
perm[378],"148",int
perm[379],"175",int
perm[380],"179",int
perm[381],"172",int
perm[382],"169",int
perm[383],"177",int
perm[384],"183",int
perm[385],"167",int
perm[386],"168",int
perm[387],"184",int
perm[388],"186",int
perm[389],"189",int
perm[390],"191",int
perm[391],"161",int
perm[392],"188",int
perm[393],"192",int
perm[394],"185",int
perm[395],"182",int
perm[396],"190",int
perm[397],"196",int
perm[398],"180",int
perm[399],"181",int
perm[400],"197",int
perm[401],"199",int
perm[402],"202",int
perm[403],"204",int
perm[404],"174",int
perm[405],"201",int
perm[406],"205",int
perm[407],"198",int
perm[408],"195",int
perm[409],"203",int
perm[410],"209",int
perm[411],"193",int
perm[412],"194",int
perm[413],"210",int
perm[414],"212",int
perm[415],"215",int
perm[416],"217",int
perm[417],"187",int
perm[418],"214",int
perm[419],"218",int
perm[420],"211",int
perm[421],"208",int
perm[422],"216",int
perm[423],"222",int
perm[424],"206",int
perm[425],"207",int
perm[426],"223",int
perm[427],"225",int
perm[428],"228",int
perm[429],"230",int
perm[430],"200",int
perm[431],"227",int
perm[432],"231",int
perm[433],"224",int
perm[434],"221",int
perm[435],"229",int
perm[436],"235",int
perm[437],"219",int
perm[438],"220",int
perm[439],"236",int
perm[440],"238",int
perm[441],"241",int
perm[442],"243",int
perm[443],"213",int
perm[444],"240",int
perm[445],"244",int
perm[446],"237",int
perm[447],"234",int
perm[448],"242",int
perm[449],"248",int
perm[450],"232",int
perm[451],"233",int
perm[452],"249",int
perm[453],"251",int
perm[454],"254",int
perm[455],"0",int
perm[456],"239",int
perm[457],"255",int
perm[458],"1",int
perm[459],"253",int
perm[460],"252",int
perm[461],"2",int
perm[462],"3",int
perm[463],"4",int
perm[464],"5",int
perm[465],"6",int
perm[466],"7",int
perm[467],"8",int
perm[468],"9",int
perm[469],"10",int
perm[470],"11",int
perm[471],"12",int
perm[472],"13",int
perm[473],"14",int
perm[474],"15",int
perm[475],"16",int
perm[476],"17",int
perm[477],"18",int
perm[478],"19",int
perm[479],"20",int
perm[480],"21",int
perm[481],"22",int
perm[482],"23",int
perm[483],"24",int
perm[484],"25",int
perm[485],"26",int
perm[486],"27",int
perm[487],"28",int
perm[488],"29",int
perm[489],"30",int
perm[490],"31",int
perm[491],"32",int
perm[492],"33",int
perm[493],"34",int
perm[494],"35",int
perm[495],"36",int
perm[496],"37",int
perm[497],"38",int
perm[498],"39",int
perm[499],"40",int
perm[500],"41",int
perm[501],"42",int
perm[502],"43",int
perm[503],"44",int
perm[504],"45",int
perm[505],"46",int
perm[506],"47",int
perm[507],"226",int
perm[508],"245",int
perm[509],"246",int
perm[510],"247",int
perm[511],"250",int
    """.trimIndent()

    val todo = tada.lines().mapNotNull {
        if (it == "") return@mapNotNull null
        val f1 = it.substring(it.indexOf('"') + 1)
        val i2 = f1.indexOf('"')
        val f2 = f1.substring(0, i2)
        f2.toInt()
    }

    list.zip(todo).forEach { (a, b) ->
        if (a != b)
            println("$a != $b")
    }

    //println(todo)

    val F4 = ((Math.sqrt(5.0) - 1.0) / 4.0).toFloat()
    val G4 = ((5.0f - Math.sqrt(5.0)) / 20.0).toFloat()
    println(F4)
    println(G4)
}
