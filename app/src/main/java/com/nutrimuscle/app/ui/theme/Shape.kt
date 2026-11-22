package com.nutrimuscle.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 28.dp
val FlatElevation = 0.dp

val NutriShapes = Shapes(
    extraSmall = CircleShape,
    small = CircleShape,
    medium = RoundedCornerShape(CardCornerRadius),
    large = RoundedCornerShape(CardCornerRadius),
    extraLarge = RoundedCornerShape(CardCornerRadius)
)
