import org.junit.*
import com.lesfurets.jenkins.unit.BasePipelineTest
import static groovy.test.GroovyAssert.*

/**
 * test /vars/whenOrSkip.groovy
 */
class WhenOrSkipTest extends BasePipelineTest {

  def whenOrSkip

    @Before
  void setUp() {
    super.setUp()
    // load whenOrSkip
    whenOrSkip = loadScript('vars/whenOrSkip.groovy')
  }

    @Test
  void testCall() {
    // call whenOrSkip and check result
    String result = whenOrSkip(true)
    assert result == 'abc3'
  }
}
