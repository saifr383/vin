package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.brother.ptouch.sdk.printdemo.model.printsettings.ISimplePrintSettings
import com.brother.ptouch.sdk.printdemo.model.printsettings.MWModelPrintSettings
import com.brother.ptouch.sdk.printdemo.model.printsettings.PJModelPrintSettings
import com.brother.ptouch.sdk.printdemo.model.printsettings.PTModelPrintSettings
import com.brother.ptouch.sdk.printdemo.model.printsettings.QLModelPrintSettings
import com.brother.ptouch.sdk.printdemo.model.printsettings.RJModelPrintSettings
import com.brother.ptouch.sdk.printdemo.model.printsettings.TDModelPrintSettings

import com.brother.sdk.lmprinter.Channel
import com.brother.sdk.lmprinter.NetworkSearchOption
import com.brother.sdk.lmprinter.OpenChannelError
import com.brother.sdk.lmprinter.PrintError
import com.brother.sdk.lmprinter.PrinterDriverGenerator
import com.brother.sdk.lmprinter.PrinterModel
import com.brother.sdk.lmprinter.PrinterSearcher
import com.example.myapplication.databinding.FragmentPrinterListBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

class PrinterListFragment :BottomSheetDialogFragment() {
    companion object {
        const val KeyPrinterInterface = "printer_interface"
        const val KeyDstFragmentId = "src_fragment_id"
        const val KeyPrinterInfo = "printer_info"
        private const val ActionUSBPermission = "com.android.example.USB_PERMISSION"
        private val MimeTypes = arrayOf("application/pdf")
    }

    private lateinit var viewModel: PrinterListViewModel
    private lateinit var binding: FragmentPrinterListBinding
    private var waitingDialog: AlertDialog? = null
    private var cancelDialog: AlertDialog? = null


    private fun showSelectPDFView() {
        dismiss();
        val isMultiple = viewModel.currentPDFData?.type == PDFPrintType.Files
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, MimeTypes)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultiple)
        }
        launcher.launch(intent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != AppCompatActivity.RESULT_OK) {
            return@registerForActivityResult
        }
        val context = context ?: return@registerForActivityResult
        val data = viewModel.currentPDFData ?: return@registerForActivityResult

        it.data?.data?.let { uri ->
            val temp = StorageUtils.getSelectFileUri(context, uri) ?: return@let
            data.pdfData.add(temp)
        }

        // multiple
        val clipData = it.data?.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val temp = StorageUtils.getSelectFileUri(context, clipData.getItemAt(i).uri) ?: continue
                data.pdfData.add(temp)
            }
        }

        if (data.pdfData.isEmpty()) {
            return@registerForActivityResult
        }


            viewModel.startToPrintPDF(context, data,{})



        // remove listener
//        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>(SelectorFragment.KeySelectedMenu)
//
//        if (viewModel.currentPDFData?.type == PDFPrintType.Pages) {
//            findNavController().navigate(PrintPDFFragmentDirections.toInputPages().apply {
//                arguments.putParcelable(PrintSettingsFragment.KeyPrintData, data)
//            })
//        } else {
//            findNavController().navigate(PrintPDFFragmentDirections.toPrintSettings().apply {
//                arguments.putParcelable(PrintSettingsFragment.KeyPrintData, data)
//                arguments.putBoolean(PrintSettingsFragment.KeyIsShowPrintBtn, true)
//            })
//        }
//    }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_printer_list, container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[PrinterListViewModel::class.java]
        setupViews()



            val context = context ?: return

            viewModel.setPrinterType("Network", context)






        startSearch()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopSearch()

    }

    private fun setupViews() {
        val context = this.context ?: return

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.printerListRecycler.layoutManager = LinearLayoutManager(context)
        binding.printerListRecycler.addItemDecoration(RecyclerViewDivider())
        binding.printerListRecycler.adapter = PrinterListRecyclerAdapter(
            context,
            viewModel.printerList,
            ::onItemSelected
        )

//        binding.printerListRefreshLayout.setOnRefreshListener {
//            startSearch()
//            binding.printerListRefreshLayout.isRefreshing = false
//        }


    }

    private fun onItemSelected(info: IPrinterInfo) {
        val controller = findNavController()
        with(controller) {
            val dstFragmentId = arguments?.getInt(KeyDstFragmentId, -1) ?: -1
            if (dstFragmentId != -1) {
                popBackStack(dstFragmentId, false)
            } else {
                // back to previous fragment
                popBackStack()
            }
           PrintDemoApp.instance.currentSelectedPrinter = info
            viewModel.printerModelName=info.modelName
            showSelectPDFView()
            currentBackStackEntry?.savedStateHandle?.set(KeyPrinterInfo, info)
        }
    }

    private fun startSearch() {

        if (waitingDialog?.isShowing == true || cancelDialog?.isShowing == true) {
            return
        }

        val result = checkPermission()
        if (!result.first) {
            binding.printerListRecycler.visibility = View.GONE
            binding.printerListText.text = getWarningMessage(result.second)
            return
        }

        context?.apply {

            binding.printerListRecycler.visibility = View.VISIBLE

            viewModel.search(this) { error, sdkError ->
                error?.let {
                    if (it != PrinterSearchError.None || viewModel.printerList.isEmpty()) {

                        binding.printerListRecycler.visibility = View.GONE
                        binding.printerListText.text = this.getString(it.getResId())
                    } else {
                        binding.printerListRecycler.visibility = View.VISIBLE
                        binding.printerListRecycler.adapter?.notifyDataSetChanged()
                    }
                }

                sdkError?.let {

                    if (it != com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.NoError) {
if(it.name!="Canceled")
                        Toast.makeText(this, it.name, Toast.LENGTH_LONG).show()
                    }
                }
            }

            binding.printerListRecycler.adapter?.notifyDataSetChanged()
        }
    }

    private fun checkPermission(): Pair<Boolean, Array<String>> {

        return Pair(true, arrayOf())
    }


    private fun getWarningMessage(permissions: Array<String>): String {
        var str = this.getString(R.string.no_permission)
        permissions.forEach {
            str += "\n$it"
        }

        return str
    }
}

object StorageUtils {
    fun getInternalFolder(context: Context): String {
        return context.filesDir.absolutePath
    }

    fun getExternalFolder(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: ""
    }

    fun getSelectFileUri(context: Context, uri: Uri): String? {
        return kotlin.runCatching {
            val file = File(selectFileTemDir(context, uri))
            if (file.exists()) {
                file.delete()
            }
            file.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.path
        }.getOrNull()
    }

    fun deleteSelectFolder(context: Context) {
        File(context.externalCacheDir?.path + File.separator + "select").delete()
    }

    private fun selectFileTemDir(context: Context, src: Uri): String {
        return context.externalCacheDir?.path + File.separator + "select" + File.separator + src.originalFileName(context)
    }

    private fun Uri.originalFileName(context: Context): String = when (scheme) {
        "content" -> {
            context.contentResolver.query(this, null, null, null, null, null)?.use {
                if (!it.moveToFirst()) return@use ""
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return@use if (index < 0) "" else it.getString(index)
            } ?: ""
        }
        else -> {
            this.lastPathSegment ?: ""
        }
    }

    fun Uri.hasFileWithExtension(extension: String, context: Context): Boolean {
        return originalFileName(context).endsWith(".$extension", true)
    }
}

@Parcelize
class PdfPrintData(
    val type: PDFPrintType,
    var pages: ArrayList<Int>,
    var pdfData: ArrayList<String>
) : IPrintData, Parcelable
enum class PDFPrintType {
    File, Files, Pages
}interface IPrintData {
    // TODO
}

enum class PrintSettingsItemType(val stringId: Int) {
    AUTO_CUT(R.string.auto_cut),
    AUTO_CUT_FOR_EACH_PAGE_COUNT(R.string.auto_cut_for_each_page_count),
    BI_COLOR_BLUE_ENHANCEMENT(R.string.bi_color_blue_enhancement),
    BI_COLOR_GREEN_ENHANCEMENT(R.string.bi_color_green_enhancement),
    BI_COLOR_RED_ENHANCEMENT(R.string.bi_color_red_enhancement),
    CHAIN_PRINT(R.string.chain_print),
    CHANNEL_TYPE(R.string.channel_type),
    COMPRESS_MODE(R.string.compress_mode),
    CUSTOM_PAPER_SIZE(R.string.custom_paper_size),
    CUTMARK_PRINT(R.string.cutmark_print),
    CUT_AT_END(R.string.cut_at_end),
    CUT_PAUSE(R.string.cut_pause),
    DENSITY(R.string.density),
    EXTRA_FEED_DOTS(R.string.extra_feed_dots),
    FEED_MODE(R.string.feed_mode),
    FORCE_STRETCH_PRINTABLE_AREA(R.string.force_stretch_printable_area),
    FORCE_VANISHING_MARGIN(R.string.force_vanishing_margin),
    HALFTONE(R.string.halftone),
    HALFTONE_THRESHOLD(R.string.halftone_threshold),
    HALF_CUT(R.string.half_cut),
    HORIZONTAL_ALIGNMENT(R.string.horizontal_alignment),
    LABEL_SIZE(R.string.label_size),
    MIRROR_PRINT(R.string.mirror_print),
    NUM_COPIES(R.string.num_copies),
    ORIENTATION(R.string.orientation),
    PAPER_INSERTION_POSITION(R.string.paper_insertion_position),
    PAPER_SIZE(R.string.paper_size),
    PAPER_TYPE(R.string.paper_type),
    PEEL_LABEL(R.string.peel_label),
    PRINTER_MODEL(R.string.printer_model),
    PRINT_DASH_LINE(R.string.print_dash_line),
    PRINT_QUALITY(R.string.print_quality),
    PRINT_SPEED(R.string.print_speed),
    RESOLUTION(R.string.resolution),
    ROLL_CASE(R.string.roll_case),
    ROTATE180DEGREES(R.string.rotate180degrees),
    ROTATION(R.string.rotation),
    SCALE_MODE(R.string.scale_mode),
    SCALE_VALUE(R.string.scale_value),
    SKIP_STATUS_CHECK(R.string.skip_status_check),
    SPECIAL_TAPE_PRINT(R.string.special_tape_print),
    USING_CARBON_COPY_PAPER(R.string.using_carbon_copy_paper),
    VERTICAL_ALIGNMENT(R.string.vertical_alignment),
    WORK_PATH(R.string.work_path),
}
class PrinterListViewModel : ViewModel(), CoroutineScope {
    var printerModelName: String? = null
    private fun getPrinterModel(name: String?): PrinterModel? {
        return PrinterModel.values().firstOrNull { it.name == name }
    }

    fun getOptionsInfoList(context: Context): ISimplePrintSettings? {
        val modelName = getPrinterModel(printerModelName)
//        if (printSettings != null && modelName == printSettings?.printerModel) {
//            return printSettings?.settingsMap ?: mutableMapOf()
//        }
        modelName?.let {
            if (it.name.startsWith("PJ")) {
                printSettings = PJModelPrintSettings(context, it)
            } else if (it.name.startsWith("MW")) {
                printSettings = MWModelPrintSettings(context, it)
            } else if (it.name.startsWith("RJ")) {
                printSettings = RJModelPrintSettings(context, it)
            } else if (it.name.startsWith("QL")) {
                printSettings = QLModelPrintSettings(context, it)
            } else if (it.name.startsWith("TD")) {
                printSettings = TDModelPrintSettings(context, it)
            } else if (it.name.startsWith("PT")) {
                printSettings = PTModelPrintSettings(context, it)
            }
        }
        return printSettings
    }
    var currentPDFData: PdfPrintData? = null

    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    var printerType: Channel.ChannelType? = null
    var printerList: ArrayList<IPrinterInfo> = arrayListOf()

    var printerSearcher: IPrinterSearcher? = null
    var printSettings: ISimplePrintSettings? = null
    fun startToPrintPDF(context: Context, printData: PdfPrintData, callback: (String) -> Unit) {
        val currentPrint = PrintDemoApp.instance.currentSelectedPrinter ?: return
        val printSettings = getOptionsInfoList(context)
        printSettings?.let {
           startPrint(context, printData, currentPrint, it, callback)
        }
    }
 fun startPrint(
     context: Context,
     printData: IPrintData,
     printerInfo: IPrinterInfo,
     printerSettings: ISimplePrintSettings,
     callback: (String) -> Unit
    ) {
        // run in thread
        launch {
            if (printData !is PdfPrintData) {
                withContext(Dispatchers.Main) {
                    callback(context.getString(R.string.wrong_print_data_type))
                }
                return@launch
            }

            val result =  printPages(context, printData, printerInfo, printerSettings)

            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
    fun setPrinterType(text: String, context: Context) {

        printerType = when (text) {
            context.getString(Channel.ChannelType.Wifi.getResId()) -> Channel.ChannelType.Wifi
            context.getString(Channel.ChannelType.Bluetooth.getResId()) -> Channel.ChannelType.Bluetooth
            context.getString(Channel.ChannelType.BluetoothLowEnergy.getResId()) -> Channel.ChannelType.BluetoothLowEnergy
            context.getString(Channel.ChannelType.USB.getResId()) -> Channel.ChannelType.USB
            else -> null
        }

    }
    private fun printPages(context: Context, printData: PdfPrintData, printerInfo: IPrinterInfo, printSettings: ISimplePrintSettings): String {
        val channel = Channel.newWifiChannel(printerInfo.ipv4Address) ?: return context.getString(R.string.create_channel_error)
        val path = printData.pdfData.firstOrNull() ?: return context.getString(R.string.no_print_data)
        val settings = printSettings ?: return context.getString(R.string.no_print_settings)
        val pages = printData.pages.toIntArray()

        val driverResult = PrinterDriverGenerator.openChannel(channel)
        if (driverResult.error.code != OpenChannelError.ErrorCode.NoError) {
            return driverResult.error.code.toString()
        }
        val driver = driverResult.driver
//        cancelRoutine = {
//            driver.cancelPrinting()
//            driver.closeChannel()
//        }

        val error = kotlin.runCatching {
            driver.printPDF(path, pages, settings.getPrintSettings())
        }.getOrElse {
            PrintError.ErrorCode.PDFPageError
        }
        driver.closeChannel()
//        cancelRoutine = null

        return when (error) {
            is PrintError -> error.code.toString() + "\r\n" + error.allLogs.joinToString("\r\n")
            is PrintError.ErrorCode -> error.toString()
            else -> ""
        }
    }


    fun stopSearch() {
        printerSearcher?.cancel()
    }

    fun search(
        context: Context,
        callback: (PrinterSearchError?, com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode?) -> Unit
    ) {
        printerList.clear()
        val searcher = getSearcher(printerType)
        searchAllModels(context, searcher, callback)
    }

    fun cancelSearching() {
        printerSearcher?.cancel()
    }

    private fun getSearcher(type: Channel.ChannelType?): IPrinterSearcher? {
        if (printerSearcher != null) {
            return printerSearcher
        }

        val target = WiFiPrinterSearcher()

        printerSearcher = target
        return target
    }

    private fun searchAllModels(
        context: Context,
        searcher: IPrinterSearcher?,
        callback: (PrinterSearchError?, com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode?) -> Unit
    ) {
        if (searcher == null) {
            callback(PrinterSearchError.None, null)
            return
        }

        val models = PrinterModel.values().map { it.name.replace('_', '-') }.toTypedArray()
        searcher.start(context, models, callback = { err, sdkError, data ->
            printerList.addAll(data)
            callback.invoke(err, sdkError)
        })
    }
}
interface IPrinterInfo {
    abstract val ipv4Address: String?
    var channelType: Channel.ChannelType
    var modelName: String

    fun getPrinterModel(): PrinterModel? {
        return guessPrinterModel(modelName)
    }
}

fun guessPrinterModel(modelName: String): PrinterModel? {
    var model: PrinterModel? = null
    PrinterModel.values().forEach {
        if (modelName.lowercase().contains(it.name.lowercase().replace("_", "-"))) {
            if (it.name.length >= (model?.name?.length ?: 0)) {
                model = it
            }
        }
    }
    return model
}

interface IPrinterSearcher {
    fun start(
        context: Context,
        targetModels: Array<String>,
        callback: (PrinterSearchError?, com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode?, ArrayList<IPrinterInfo>) -> Unit
    )

    fun cancel()
}
enum class PrinterSearchError {
    None,
    WiFiOff,
    WiFiNotConnect,
    BluetoothOff,
    USBPermissionNotGrant;

    fun getResId(): Int {
        return when (this) {
            None -> R.string.not_find_data
            WiFiOff -> R.string.wifi_off
            WiFiNotConnect -> R.string.wifi_not_connect
            BluetoothOff -> R.string.bluetooth_off
            USBPermissionNotGrant -> R.string.usb_permission
        }
    }
}



class WiFiPrinterSearcher : IPrinterSearcher, CoroutineScope {
    companion object {
        const val searchDurationSeconds = 15.0
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    private var cancelRoutine: (() -> Unit)? = null

    override fun start(
        context: Context,
        targetModels: Array<String>,
        callback: (PrinterSearchError?, com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode?, ArrayList<IPrinterInfo>) -> Unit
    ) {

        if (!NetworkUtils.isWiFiOn(context)) {
            callback.invoke(PrinterSearchError.WiFiOff, null, arrayListOf())
            return
        }

        if (!NetworkUtils.isWiFiConnected(context)) {
            callback.invoke(PrinterSearchError.WiFiNotConnect, null, arrayListOf())
            return
        }

        launch {
            val option = NetworkSearchOption(searchDurationSeconds, false)
            cancelRoutine = {
                PrinterSearcher.cancelNetworkSearch()
            }
            val result = PrinterSearcher.startNetworkSearch(context, option) { channel ->
                val info = WiFiPrinterInfo(
                    channel.extraInfo[Channel.ExtraInfoKey.ModelName] ?: "",
                    channel.channelInfo
                )
                launch(Dispatchers.Main) {
                    callback(PrinterSearchError.None, null, arrayListOf(info))
                }
            }
            cancelRoutine = null

            withContext(Dispatchers.Main) {
                callback(null, result.error.code, arrayListOf())
            }
        }
    }

    override fun cancel() {
        launch {
            cancelRoutine?.invoke()
            cancelRoutine = null
        }
    }
}
object NetworkUtils {
    fun isWiFiOn(context: Context): Boolean {
        // require permission：ACCESS_WIFI_STATE
        val manger = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return manger.wifiState == WifiManager.WIFI_STATE_ENABLED
    }

    fun isWiFiConnected(context: Context): Boolean {
        // require permission：ACCESS_NETWORK_STATE
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
            return this.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }

        return false
    }
}

class RecyclerViewDivider : RecyclerView.ItemDecoration() {
    private val dividerHeight = 1
    private val dividerColor = Color.LTGRAY
    private val paint: Paint = Paint().apply {
        color = dividerColor
        strokeWidth = dividerHeight.toFloat()
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val position: Int = parent.getChildAdapterPosition(view)
        view.tag = position

        val manager = parent.layoutManager
        // only for vertical linearLayout
        if (manager is LinearLayoutManager && position != 0 && manager.orientation == LinearLayoutManager.VERTICAL) {
            outRect.top = dividerHeight
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        val count: Int = parent.childCount
        val left: Int = parent.paddingLeft
        val right: Int = parent.measuredWidth - parent.paddingRight

        val manager = parent.layoutManager
        if (manager is LinearLayoutManager && manager.orientation == LinearLayoutManager.VERTICAL) {
            for (index in 0 until count) {
                val view: View = parent.getChildAt(index)
                val layoutParams = view.layoutParams as RecyclerView.LayoutParams
                val top = view.bottom + layoutParams.bottomMargin
                val bottom: Int = top + dividerHeight
                c.drawLine(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
    }
}



