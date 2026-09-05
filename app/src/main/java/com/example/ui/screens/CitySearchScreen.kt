package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CityLocation
import com.example.ui.theme.*

@Composable
fun CitySearchScreen(
    searchQuery: String,
    searchResults: List<CityLocation>,
    isSearching: Boolean,
    favoriteCities: List<CityLocation>,
    currentLocation: CityLocation,
    onQueryChange: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onAutoDetectGps: () -> Unit,
    onToggleFavorite: (CityLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("city_search_view")
    ) {
        // Header
        Text(
            text = stringResource(R.string.search_header_title),
            color = SleekTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = stringResource(R.string.search_header_subtitle),
            color = SleekTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input Field & GPS Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder), color = SleekTextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search), tint = SleekBluePrimary)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SleekBluePrimary, strokeWidth = 2.dp)
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_clear), tint = SleekTextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SleekBluePrimary,
                    unfocusedBorderColor = SleekBorder,
                    focusedTextColor = SleekTextPrimary,
                    unfocusedTextColor = SleekTextPrimary,
                    focusedContainerColor = SleekSurface,
                    unfocusedContainerColor = SleekSurfaceSecondary
                ),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .weight(1f)
                    .testTag("city_search_input")
            )

            // Auto-Detect GPS Button
            Button(
                onClick = onAutoDetectGps,
                colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("auto_detect_gps_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.search_gps_button), tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.search_gps_button),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Results List (if query active)
        if (searchQuery.trim().length >= 2) {
            Text(
                text = stringResource(R.string.search_results_title, searchResults.size),
                color = SleekBluePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (searchResults.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.search_no_results, searchQuery), color = SleekTextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults) { city ->
                        CityResultCard(
                            city = city,
                            isCurrent = city.name.equals(currentLocation.name, ignoreCase = true),
                            isFav = favoriteCities.any { it.name.equals(city.name, ignoreCase = true) },
                            onSelect = { onSelectCity(city) },
                            onToggleFav = { onToggleFavorite(city) }
                        )
                    }
                }
            }
        } else {
            // Bookmarked Favorite Cities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_saved_title, favoriteCities.size),
                    color = SleekBluePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = stringResource(R.string.search_saved_subtitle),
                    color = SleekTextSecondary,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(favoriteCities) { city ->
                    CityResultCard(
                        city = city,
                        isCurrent = city.name.equals(currentLocation.name, ignoreCase = true),
                        isFav = true,
                        onSelect = { onSelectCity(city) },
                        onToggleFav = { onToggleFavorite(city) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CityResultCard(
    city: CityLocation,
    isCurrent: Boolean,
    isFav: Boolean,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) SleekBlueContainer else SleekSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrent) SleekBluePrimary else SleekBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("city_card_${city.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) SleekBluePrimary.copy(alpha = 0.15f) else SleekSurfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (city.isGpsDetected) Icons.Default.MyLocation else Icons.Default.LocationCity,
                        contentDescription = stringResource(R.string.common_city),
                        tint = if (isCurrent) SleekBluePrimary else SleekTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = city.name,
                            color = if (isCurrent) SleekOnBlueContainer else SleekTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = SleekGreenContainer) {
                                Text(
                                    text = stringResource(R.string.search_active_badge),
                                    color = SleekGreenText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${if (city.region.isNotBlank()) "${city.region}, " else ""}${city.country} (${city.countryCode})",
                        color = SleekTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Lat: ${String.format(java.util.Locale.US, "%.2f", city.latitude)}, Lon: ${String.format(java.util.Locale.US, "%.2f", city.longitude)}",
                        color = SleekTextSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(R.string.common_favorite),
                    tint = if (isFav) SolarGold else SleekTextSecondary
                )
            }
        }
    }
}
