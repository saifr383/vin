//package com.example.myapplication
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Rect
//import android.os.Bundle
//import android.os.Parcelable
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import androidx.databinding.DataBindingUtil
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.myapplication.databinding.FragmentBaseListBinding
//import com.example.myapplication.databinding.ItemRecyclerCategoryBinding
//import com.example.myapplication.databinding.ItemRecyclerItemBinding
//import com.example.myapplication.databinding.ItemSimpleStringBinding
//import kotlinx.parcelize.Parcelize
//
//
//class PrinterInterfaceFragment : Fragment() {
//    private lateinit var viewModel: PrinterInterfaceViewModel
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.fragment_container, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        viewModel = ViewModelProvider(this)[PrinterInterfaceViewModel::class.java]
//        setupView()
//        setupEvents()
//    }
//
//    private fun setupView() {
//        val context = context ?: return
//
//        val bundle = Bundle().apply {
//            putInt("title.id", R.string.select_printer_interface)
//            putStringArrayList("menu.list", ArrayList(viewModel.getPrinterTypeList(context)))
//        }
//
//        val fragment = SelectorFragment().apply {
//            arguments = bundle
//        }
//
//        childFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
//    }
//
//    private fun setupEvents() {
//        val handle = findNavController().currentBackStackEntry?.savedStateHandle
//        handle?.getLiveData<String>(SelectorFragment.KeySelectedMenu)?.observe(viewLifecycleOwner) {
//            handle.remove<String>(SelectorFragment.KeySelectedMenu)
//
//            findNavController().navigate(R.id.fragment_printer_list, Bundle().apply {
//                this.putString(PrinterListFragment.KeyPrinterInterface, it)
//            })
//        }
//
//        handle?.getLiveData<IPrinterInfo>(PrinterListFragment.KeyPrinterInfo)?.observe(viewLifecycleOwner) {
//            handle.remove<IPrinterInfo>(PrinterListFragment.KeyPrinterInfo)
//
//            findNavController().popBackStack()
//            findNavController().currentBackStackEntry?.savedStateHandle?.set(PrinterListFragment.KeyPrinterInfo, it)
//        }
//    }
//}
//
//
//open class SelectorFragment : Fragment() {
//    companion object {
//        const val KeyTitleId = "title.id"
//        const val KeyMenuList = "menu.list"
//        const val KeyButtonMenuList = "menu.button.list"
//        const val KeyItemGravity = "item.gravity"
//        const val KeySelectedMenu = "selected.menu"
//        const val KeySelectedButtonMenu = "selected.button.menu"
//    }
//
//    lateinit var binding: FragmentBaseListBinding
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val context = this.context ?: return super.onCreateView(inflater, container, savedInstanceState)
//        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.fragment_base_list, null, false)
//        binding.lifecycleOwner = this.viewLifecycleOwner
//
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupViews()
//    }
//
//    @Suppress("DEPRECATION")
//    private fun setupViews() {
//        val context = this.context ?: return
//
//        arguments?.getInt(KeyTitleId)?.let {
//            binding.toolbar.title = context.getString(it)
//        }
//
//        val stringItems = arguments?.getStringArrayList(KeyMenuList) ?: listOf<String>()
//        val buttonItems = arguments?.getParcelableArrayList(KeyButtonMenuList) ?: listOf<SimpleData>()
//        val itemGravity = arguments?.getInt(KeyItemGravity, Gravity.CENTER_HORIZONTAL) ?: Gravity.CENTER_HORIZONTAL
//
//        // recycler view
//        binding.baseRecyclerView.layoutManager = LinearLayoutManager(context)
//
//        if (stringItems.isNotEmpty()) {
//            binding.baseRecyclerView.addItemDecoration(RecyclerViewDivider())
//            binding.baseRecyclerView.adapter = SimpleStringRecyclerAdapter(
//                context,
//                stringItems,
//                onItemSelected = ::onItemSelected,
//                itemGravity
//            )
//        } else {
//            binding.baseRecyclerView.adapter =
//                SimpleCategoryButtonRecyclerAdapter(context, buttonItems, onItemSelected = ::onButtonItemSelected)
//        }
//
//        binding.toolbar.setNavigationOnClickListener {
//            findNavController().popBackStack()
//        }
//    }
//
//    private fun onItemSelected(info: String) {
//        val controller = findNavController()
//        with(controller) {
//            currentBackStackEntry?.savedStateHandle?.set(KeySelectedMenu, info)
//        }
//    }
//
//    private fun onButtonItemSelected(data: SimpleData) {
//        val controller = findNavController()
//        with(controller) {
//            currentBackStackEntry?.savedStateHandle?.set(KeySelectedButtonMenu, data)
//        }
//    }
//}@Parcelize
//data class SimpleData(val isCategory: Boolean, val info: String) : Parcelable
//
//
//
//
//class RecyclerViewDivider : RecyclerView.ItemDecoration() {
//    private val dividerHeight = 1
//    private val dividerColor = Color.LTGRAY
//    private val paint: Paint = Paint().apply {
//        color = dividerColor
//        strokeWidth = dividerHeight.toFloat()
//        style = Paint.Style.STROKE
//        isAntiAlias = true
//    }
//
//    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//        super.getItemOffsets(outRect, view, parent, state)
//
//        val position: Int = parent.getChildAdapterPosition(view)
//        view.tag = position
//
//        val manager = parent.layoutManager
//        // only for vertical linearLayout
//        if (manager is LinearLayoutManager && position != 0 && manager.orientation == LinearLayoutManager.VERTICAL) {
//            outRect.top = dividerHeight
//        }
//    }
//
//    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
//        super.onDraw(c, parent, state)
//        val count: Int = parent.childCount
//        val left: Int = parent.paddingLeft
//        val right: Int = parent.measuredWidth - parent.paddingRight
//
//        val manager = parent.layoutManager
//        if (manager is LinearLayoutManager && manager.orientation == LinearLayoutManager.VERTICAL) {
//            for (index in 0 until count) {
//                val view: View = parent.getChildAt(index)
//                val layoutParams = view.layoutParams as RecyclerView.LayoutParams
//                val top = view.bottom + layoutParams.bottomMargin
//                val bottom: Int = top + dividerHeight
//                c.drawLine(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
//            }
//        }
//    }
//}
//
//
//class SimpleStringRecyclerAdapter(
//    val context: Context,
//    private var data: List<String>,
//    private val onItemSelected: (String) -> Unit,
//    private val gravity: Int = Gravity.START
//) : RecyclerView.Adapter<SimpleStringRecyclerAdapter.SimpleStringViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleStringViewHolder {
//        val binding: ItemSimpleStringBinding =
//            DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.item_simple_string, parent, false)
//
//        // set layout_gravity of textview
//        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        params.gravity = gravity
//        binding.simpleTextView.layoutParams = params
//
//        return SimpleStringViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: SimpleStringViewHolder, position: Int) {
//        val info = data.getOrNull(position) ?: return
//        holder.binding.simpleTextView.text = info
//        holder.binding.simpleRoot.setOnClickListener {
//            onItemSelected(info)
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return data.size
//    }
//
//    class SimpleStringViewHolder(val binding: ItemSimpleStringBinding) : RecyclerView.ViewHolder(binding.root)
//}
//
//class SimpleCategoryButtonRecyclerAdapter(
//    private val context: Context,
//    private val data: List<SimpleData>,
//    private val onItemSelected: (SimpleData) -> Unit
//) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    companion object {
//        private const val TypeCategory = 0
//        private const val TypeItem = 1
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        val info = data.getOrNull(position)
//        return if (info?.isCategory == true) TypeCategory else TypeItem
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        val inflater = LayoutInflater.from(context)
//
//        return if (viewType == TypeCategory) {
//            CategoryViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_recycler_category, parent, false))
//        } else {
//            ItemViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_recycler_item, parent, false))
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val info = data.getOrNull(position) ?: return
//        if (holder is CategoryViewHolder) {
//            holder.binding.menuCategoryTitle.text = info.info
//            return
//        }
//
//        if (holder is ItemViewHolder) {
//            holder.binding.menuItemButton.text = info.info
//            holder.binding.menuItemButton.setOnClickListener {
//
//
//                onItemSelected(info)
//            }
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return data.size
//    }
//
//    class CategoryViewHolder(val binding: ItemRecyclerCategoryBinding) : RecyclerView.ViewHolder(binding.root)
//    class ItemViewHolder(val binding: ItemRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root)
//}
