import 'dart:convert';

class Entry {
  final int? id;
  final dynamic begin;
  final dynamic end;
  final Map<String, dynamic> inputs;
  final int? caseId;
  final String? media;
  final String media_name;
  final dynamic createdAt;
  final dynamic updatedAt;
  final String noCase;
  final dynamic audio;
  final dynamic image;

  const Entry({
    this.id,
    this.begin,
    this.end,
    this.inputs = const {},
    this.caseId,
    this.media,
    this.media_name = '',
    this.createdAt,
    this.updatedAt,
    this.noCase = 'false',
    this.audio,
    this.image,
  });

  Map<String, dynamic> toJson() => {
        "id": id,
        "begin": begin?.toString(),
        "end": end?.toString(),
        "inputs": inputs,
        "case_id": caseId,
        "media_id": media, // Make sure this is properly named
        "media_name": media_name,
        "created_at": createdAt,
        "updated_at": updatedAt,
        "audio": audio,
        "image": image,
        "nocase": noCase,
      };

  // Make sure fromJson also handles media properly
  factory Entry.fromJson(Map<String, dynamic> array) => Entry(
        id: array["id"],
        begin: array["begin"],
        end: array["end"],
        inputs: array["inputs"] != null ? json.decode(array["inputs"]) : {},
        caseId: array["case_id"],
        media: array["media_id"]?.toString(), // Convert to string if needed
        media_name: array["media_name"] ?? '',
        createdAt: array["created_at"],
        updatedAt: array["updated_at"],
        audio: array["audio"],
        image: array["image"],
        noCase: array["nocase"] ?? 'false',
      );

  Entry copyWith({
    int? id,
    dynamic begin,
    dynamic end,
    Map<String, dynamic>? inputs,
    int? caseId, // Make sure this exists
    dynamic media,
    String? media_name,
    dynamic createdAt,
    dynamic updatedAt,
    String? noCase,
    dynamic audio,
    dynamic image,
  }) {
    return Entry(
      id: id ?? this.id,
      begin: begin ?? this.begin,
      end: end ?? this.end,
      inputs: inputs ?? this.inputs,
      caseId: caseId ?? this.caseId,
      media: media ?? this.media,
      media_name: media_name ?? this.media_name,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      noCase: noCase ?? this.noCase,
      audio: audio ?? this.audio,
      image: image ?? this.image,
    );
  }
}

Entry entryFromJson(String str) {
  final jsonData = json.decode(str);
  return Entry.fromJson(jsonData);
}

String entryToJson(Entry data) {
  final dyn = data.toJson();
  return json.encode(dyn);
}

List<Entry> allEntriesFromJson(String str) {
  final jsonData = json.decode(str);
  return List<Entry>.from(jsonData['data'].map((x) => Entry.fromJson(x)));
}

List<Entry> noCase(String str) {
  final nocase = json.decode('[{"nocase": "true"}]');
  return List<Entry>.from(nocase.map((x) => Entry.fromJson(x)));
}

extension EntryHelpers on Entry {
  bool get isValid => id != null && begin != null && end != null;
  bool get hasMedia => media != null;
  bool get hasAudio => audio != null;
  bool get hasImage => image != null;

  DateTime? get beginDate =>
      begin != null ? DateTime.parse(begin.toString()) : null;
  DateTime? get endDate => end != null ? DateTime.parse(end.toString()) : null;

  Duration? get duration {
    if (beginDate == null || endDate == null) return null;
    return endDate!.difference(beginDate!);
  }
}
