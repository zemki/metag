import 'package:flutter/material.dart';
import 'custom_SelectionModal.dart';

class MultiSelect extends FormField<List<dynamic>> {
  MultiSelect({
    super.key,
    super.onSaved,
    super.validator,
    super.initialValue,
    this.titleText = 'Title',
    this.hintText = 'Tap to select one or more...',
    this.required = false,
    this.errorText = 'Please select one or more option(s)',
    this.value,
    this.leading,
    this.filterable = true,
    required this.dataSource,
    required this.textField,
    required this.valueField,
    this.change,
    this.open,
    this.close,
    this.trailing,
    this.maxSelectableItems,
  }) : super(
          builder: (FormFieldState<dynamic> state) {
            List<Widget> _buildSelectedOptions(dynamic values, state) {
              List<Widget> selectedOptions = [];

              if (values != null) {
                for (var item in values) {
                  var existingItem = dataSource.firstWhere(
                    (itm) => itm[valueField] == item,
                    orElse: () => null,
                  );
                  if (existingItem != null) {
                    selectedOptions.add(
                      Chip(
                        label: Text(
                          existingItem[textField],
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    );
                  }
                }
              }

              return selectedOptions;
            }

            return InkWell(
              onTap: () async {
                final results = await Navigator.push<List<dynamic>>(
                  state.context,
                  MaterialPageRoute<List<dynamic>>(
                    builder: (BuildContext context) => SelectionModal(
                      filterable: filterable,
                      valueField: valueField,
                      textField: textField,
                      dataSource: dataSource,
                      values: state.value ?? [],
                      maxItems: maxSelectableItems ?? 100,
                    ),
                    fullscreenDialog: true,
                  ),
                );

                if (results != null) {
                  if (results.isNotEmpty) {
                    state.didChange(results);
                  } else {
                    state.didChange(null);
                  }
                }
              },
              child: InputDecorator(
                decoration: InputDecoration(
                  filled: true,
                  fillColor: Colors.white,
                  contentPadding:
                      const EdgeInsets.fromLTRB(10.0, 0.0, 10.0, 0.0),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(5.0),
                    borderSide: BorderSide(color: Colors.grey.shade400),
                  ),
                  errorText: state.hasError ? state.errorText : null,
                  errorMaxLines: 4,
                ),
                isEmpty: state.value == null || state.value == '',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Padding(
                      padding: const EdgeInsets.only(top: 8.0),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Expanded(
                            child: Text(
                              titleText,
                              style: const TextStyle(
                                fontSize: 14.0,
                                color: Colors.black,
                              ),
                            ),
                          ),
                          if (required)
                            Text(
                              ' *',
                              style: TextStyle(
                                color: Colors.red.shade700,
                                fontSize: 12.0,
                              ),
                            ),
                          const Icon(
                            Icons.arrow_downward,
                            color: Color(0xFF3D72B6),
                            size: 30.0,
                          ),
                        ],
                      ),
                    ),
                    if (state.value != null)
                      Wrap(
                        spacing: 8.0,
                        runSpacing: 1.0,
                        children: _buildSelectedOptions(state.value, state),
                      )
                    else
                      Container(
                        margin: const EdgeInsets.fromLTRB(0.0, 10.0, 0.0, 6.0),
                        child: Text(
                          hintText,
                          style: TextStyle(
                            color: Colors.grey.shade500,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        );

  final String titleText;
  final String hintText;
  final bool required;
  final String errorText;
  final dynamic value;
  final bool filterable;
  final List<dynamic> dataSource;
  final String textField;
  final String valueField;
  final Function? change;
  final Function? open;
  final Function? close;
  final Widget? leading;
  final Widget? trailing;
  final int? maxSelectableItems;
}
