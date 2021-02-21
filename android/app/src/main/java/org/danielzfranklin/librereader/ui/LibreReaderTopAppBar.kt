package org.danielzfranklin.librereader.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.danielzfranklin.librereader.R

@Composable
fun LibreReaderTopAppBar(title: String, actions: @Composable () -> Unit) {
    TopAppBar(Modifier.testTag("topAppBar")) {
        ConstraintLayout(Modifier.fillMaxWidth(1f)) {
            val (refTitle, refActions) = createRefs()

            Row(
                Modifier.padding(start = 10.dp).fillMaxHeight(1f).constrainAs(refTitle) {
                    width = Dimension.wrapContent
                    start.linkTo(parent.start)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.h6)
            }

            Row(
                Modifier.fillMaxHeight(1f).constrainAs(refActions) {
                    width = Dimension.wrapContent
                    end.linkTo(parent.end)
                }, verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
    }
}

@Preview
@Composable
fun LibrereaderTopAppBarPreview() {
    LibreReaderTopAppBar("Some Title") {
        IconButton({}) {
            Icon(painterResource(R.drawable.ic_import_book), null)
        }

        IconButton({}) {
            Icon(painterResource(R.drawable.ic_margin_larger), null)
        }
    }
}