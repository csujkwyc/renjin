#
# Renjin : JVM-based interpreter for the R language for the statistical analysis
# Copyright © 2010-2019 BeDataDriven Groep B.V. and contributors
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, a copy is available at
# https://www.gnu.org/licenses/gpl-2.0.txt
#


library(hamcrest)

test.keepsource <- function() {

    x <- parse(text = "x+y\ny+z", keep.source = TRUE)
    assertFalse(is.null(attr(x, 'srcref')))

    y <- parse(text = "x+y\ny+z", keep.source = FALSE)
    assertTrue(is.null(attr(y, 'srcref')))
}

test.parse.language <- function() {

    assertThat(parse(text=quote(x), keep.source=FALSE), identicalTo(expression(x)))
    assertThat(parse(text=quote(cbind(ph.ecog)), keep.source=FALSE), identicalTo(expression(cbind, ph.ecog)))

}