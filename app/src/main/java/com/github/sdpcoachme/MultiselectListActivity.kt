package com.github.sdpcoachme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.ListSport
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class MultiselectListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                MultiSelectList()
            }
        }
    }

    @Composable
    fun MultiSelectList() {
        var sportItems by remember {
            mutableStateOf(
                listOf("Ski", "Tennis", "Padel").map {
                    ListSport(
                        title = it,
                        selected = false
                    )
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(sportItems.size) { i ->
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sportItems = sportItems.mapIndexed { j, item ->
                                if (i == j) {
                                    item.copy(selected = !item.selected)
                                } else {
                                    item
                                }
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = sportItems[i].title)
                    if (sportItems[i].selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}