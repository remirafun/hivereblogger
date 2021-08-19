require 'hive'
require 'java'
java_import 'hiwebproject.HiWebProject'
java_import 'hiwebproject.RubyInterface'

$api = Hive::CondenserApi.new
class RubyAPI include RubyInterface
    def get_content(author, permlink)
        $api.get_content(author, permlink) do |result|
            return result
        end
        return null
    end
    def get_reblogged_by(author, permlink)
        $api.get_reblogged_by(author, permlink) do |result|
            return result
        end
        return null
    end
    def get_dynamic_global_properties() 
        $api.get_dynamic_global_properties() do |result|
            return result
        end
        return null
    end
    def start_stream() 
        stream = Hive::Stream.new
        stream.operations(types: [:comment_operation, :vote_operation]) do |op, trx_id, block_num|
            HiWebProject.incoming trx_id, block_num, op
        end
    end
    def start_stream_from(from) 
        stream = Hive::Stream.new
        stream.operations(at_block_num: from, types: [:comment_operation, :vote_operation]) do |op, trx_id, block_num|
            HiWebProject.incoming trx_id, block_num, op
        end
    end

end
