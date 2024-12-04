import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';

class SelectionModal extends StatefulWidget {
  final List<dynamic> dataSource;
  final List<dynamic> values;
  final bool filterable;
  final String textField;
  final String valueField;
  final int maxItems;

  const SelectionModal({
    super.key,
    required this.filterable,
    required this.dataSource,
    required this.values,
    required this.textField,
    required this.valueField,
    required this.maxItems,
  });

  @override
  State<SelectionModal> createState() => _SelectionModalState();
}

class _SelectionModalState extends State<SelectionModal> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  final TextEditingController _searchController = TextEditingController();
  late List<Map<String, dynamic>> _localDataSourceWithState;
  late List<Map<String, dynamic>> _searchResults;
  late bool _isSearching;

  @override
  void initState() {
    super.initState();
    _initializeData();
    _setupSearchController();
  }

  void _initializeData() {
    _isSearching = false;
    _localDataSourceWithState = widget.dataSource.map((item) {
      return {
        'value': item[widget.valueField],
        'text': item[widget.textField],
        'checked': widget.values.contains(item[widget.valueField])
      };
    }).toList();
    _searchResults = List.from(_localDataSourceWithState);
  }

  void _setupSearchController() {
    _searchController.addListener(() {
      setState(() {
        _isSearching = _searchController.text.isNotEmpty;
      });
    });
  }

  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      leading: const SizedBox(),
      elevation: 0.0,
      title: const Text('Please select one or more'),
      actions: [
        IconButton(
          icon: const Icon(Icons.close, size: 25.0),
          onPressed: () => Navigator.pop(context),
        ),
      ],
    );
  }

  Widget _buildBody() {
    return SafeArea(
      child: Column(
        children: [
          if (widget.filterable) _buildSearchField(),
          Expanded(child: _buildOptionsList()),
          _buildSelectedOptions(),
          _buildActionButtons(),
        ],
      ),
    );
  }

  Widget _buildSearchField() {
    return Container(
      color: Theme.of(context).primaryColor,
      padding: const EdgeInsets.only(left: 8.0, right: 8.0, bottom: 10.0),
      child: TextField(
        controller: _searchController,
        onChanged: _searchOperation,
        decoration: InputDecoration(
          contentPadding: const EdgeInsets.all(12.0),
          border: const OutlineInputBorder(
            borderRadius: BorderRadius.all(Radius.circular(6.0)),
          ),
          filled: true,
          hintText: "Search...",
          fillColor: Colors.white,
          suffixIcon: IconButton(
            icon: const Icon(Icons.clear),
            onPressed: () {
              _searchController.clear();
              _searchOperation('');
            },
          ),
        ),
      ),
    );
  }

  Widget _buildOptionsList() {
    return ListView.separated(
      itemCount: _searchResults.length,
      separatorBuilder: (_, __) => const Divider(height: 1.0),
      itemBuilder: (context, index) {
        final item = _searchResults[index];
        return ListTile(
          title: Text(item['text'].toString()),
          leading: Transform.scale(
            scale: 1.5,
            child: Icon(
              item['checked'] ? Icons.check_box : Icons.check_box_outline_blank,
              color: Colors.blueAccent,
            ),
          ),
          onTap: () => _toggleItem(item),
        );
      },
    );
  }

  Widget _buildSelectedOptions() {
    final selectedItems = _localDataSourceWithState
        .where((item) => item['checked'])
        .map((item) => Chip(
              label: Text(
                item['text'].toString(),
                overflow: TextOverflow.ellipsis,
              ),
              deleteIcon: const Icon(Icons.cancel),
              deleteIconColor: Colors.grey,
              onDeleted: () => _toggleItem(item),
            ))
        .toList();

    if (selectedItems.isEmpty) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(10.0),
      color: Colors.grey.shade400,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Currently selected items (tap to remove)',
            style: TextStyle(
              color: Colors.black87,
              fontWeight: FontWeight.bold,
            ),
          ),
          Wrap(
            spacing: 8.0,
            runSpacing: 0.4,
            alignment: WrapAlignment.start,
            children: selectedItems,
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Container(
      color: Colors.grey.shade600,
      child: OverflowBar(
        alignment: MainAxisAlignment.spaceBetween,
        children: [
          TextButton.icon(
            icon: const Icon(Icons.clear, color: Colors.white),
            label: const Text('Cancel', style: TextStyle(color: Colors.white)),
            onPressed: () => Navigator.pop(context),
          ),
          TextButton.icon(
            icon: const Icon(Icons.save, color: Colors.white),
            label: const Text('Save', style: TextStyle(color: Colors.white)),
            onPressed: _handleSave,
          ),
        ],
      ),
    );
  }

  void _toggleItem(Map<String, dynamic> item) {
    setState(() {
      item['checked'] = !item['checked'];
    });
  }

  void _handleSave() {
    final selectedItems = _localDataSourceWithState
        .where((item) => item['checked'])
        .map((item) => item['value'])
        .toList();

    if (widget.maxItems < selectedItems.length) {
      Fluttertoast.showToast(
        msg: "Please select only one option",
        toastLength: Toast.LENGTH_LONG,
        gravity: ToastGravity.CENTER,
        backgroundColor: Colors.red,
        textColor: Colors.white,
        fontSize: 18.0,
      );
      return;
    }

    Navigator.pop(context, selectedItems);
  }

  void _searchOperation(String searchText) {
    setState(() {
      _searchResults = _localDataSourceWithState.where((item) {
        final data = '${item['value']} ${item['text']}'.toLowerCase();
        return data.contains(searchText.toLowerCase());
      }).toList();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      appBar: _buildAppBar(),
      body: _buildBody(),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }
}
