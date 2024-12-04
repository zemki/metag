import 'package:flutter/material.dart';

class StarDisplayWidget extends StatelessWidget {
  final int value;
  final int initialValue;
  final Widget filledStar;
  final Widget unfilledStar;

  const StarDisplayWidget({
    super.key,
    this.value = 0,
    this.initialValue = 0,
    required this.filledStar,
    required this.unfilledStar,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (index) {
        return index < value ? filledStar : unfilledStar;
      }),
    );
  }
}

class StarDisplay extends StarDisplayWidget {
  const StarDisplay({super.key, int value = 0})
      : super(
          value: value,
          filledStar: const Icon(Icons.star),
          unfilledStar: const Icon(Icons.star_border),
        );
}

class StarRating extends StatelessWidget {
  final void Function(int index) onChanged;
  final int value;
  final IconData? filledStar;
  final IconData? unfilledStar;
  final int initialValue;

  const StarRating({
    super.key,
    required this.onChanged,
    this.value = 0,
    this.initialValue = 0,
    this.filledStar,
    this.unfilledStar,
  });

  @override
  Widget build(BuildContext context) {
    final color = hexToColor("#113F63");
    final white = hexToColor("#131f29");
    final size = 36.0;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (index) {
        return IconButton(
          onPressed: () {
            onChanged(value == index + 1 ? index : index + 1);
          },
          color: index < value ? color : white,
          iconSize: size,
          icon: Icon(
            index < value
                ? filledStar ?? Icons.star
                : unfilledStar ?? Icons.star_border,
          ),
          padding: EdgeInsets.zero,
          tooltip: "${index + 1} of 5",
        );
      }),
    );
  }
}

Color hexToColor(String hexString, {String alphaChannel = 'FF'}) {
  return Color(int.parse(hexString.replaceFirst('#', '0x$alphaChannel')));
}

class StatefulStarRating extends StatefulWidget {
  @override
  _StatefulStarRatingState createState() => _StatefulStarRatingState();
}

class _StatefulStarRatingState extends State<StatefulStarRating> {
  int rating = 0;

  @override
  Widget build(BuildContext context) {
    return StarRating(
      onChanged: (index) {
        setState(() {
          rating = index;
        });
      },
      value: rating,
    );
  }
}
