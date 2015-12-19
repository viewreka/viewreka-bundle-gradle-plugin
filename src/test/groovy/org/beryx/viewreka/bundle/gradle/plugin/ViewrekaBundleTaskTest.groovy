package org.beryx.viewreka.bundle.gradle.plugin

import spock.lang.Specification
import spock.lang.Unroll

import static ViewrekaBundleTask.toValidRelocator

class ViewrekaBundleTaskTest extends Specification {
    @Unroll()
    def "should convert relocator prefix '#s' to '#rel'"() {
        expect:
        toValidRelocator(s) == rel

        where:
        s                   | rel
        ''                  | ''
        null                | null
        '0'                 | '_0'
        '_1'                | '_1'
        '123'               | '_123'
        '1two3'             | '_1two3'
        'a.b.c'             | 'a.b.c'
        'a.b.c.'            | 'a.b.c'
        '.a.b.c'            | 'a.b.c'
        '...a..b....c...'   | 'a.b.c'
        'a.b-c+d*e/f^g!=h?' | 'a.b_c_d_e_f_g__h_'
    }
}
