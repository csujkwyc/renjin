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
library(methods)

setClass("Gene", representation(name="character", size="numeric"))

setGroupGeneric('SequenceAnalysis', function(x, y) NULL)
setMethod('SequenceAnalysis', signature(x = 'Gene', y = 'Gene'), function(x, y) callGeneric(x@size, y@size))

setGeneric('add', group = 'SequenceAnalysis', function(x, y) standardGeneric('add'))
setMethod('add', signature(x = 'numeric', y = 'numeric'), function(x, y) x + y)

setGeneric('devide', group = 'SequenceAnalysis', function(x, y) standardGeneric('devide'))
setMethod('devide', signature(x = 'numeric', y = 'numeric'), function(x, y) x / y)

a <- new("Gene", name="Gene1", size = 5)
b <- new("Gene", name="Gene2", size = 6)

test.group.dispatch.00 = function() { assertThat(add(a, b), identicalTo(11)) }
test.group.dispatch.01 = function() { assertThat(devide(b, a), identicalTo(1.2)) }
