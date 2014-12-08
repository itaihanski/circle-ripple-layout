circle-ripple-layout
====================

A wrapping circular layout for backwards compatible Android "Lollipop-style" circular ripple animation

![Demo Image](https://raw.github.com/itaihanski/circle-ripple-layout/master/demo.gif)

Usage
-----

Wrap a view, usually a `TextView` or and `ImageView` (the button icon or text) in the `CirclRippleLayout` in your layout XML.
Make sure that the `CircleLayoutRipple` has equal width and height (it's supposed to be circular).
Optionally add a circle or a floating FAB like circle background to the `CirclRippleLayout`.
Have the `CirclRippleLayout` handle the click events and you're good to go.
