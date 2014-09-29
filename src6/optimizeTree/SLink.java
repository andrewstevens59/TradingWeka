package optimizeTree;


public class SLink implements Comparable<SLink> {
	PolicyNode src;
	PolicyNode dst;
	SLink next_ptr = null;
	SLink prev_ptr = null;
	
	// This is used to store the embedding node
	PolicyNode embed_node = null;
	// This stores the set of related s-links on each level
	SLink level_ptr = null;
	// This stores the path update id of the s-link
	int update_id = 0;
	// This stores the optimal action reward
	float exp_rew = 0;
	
	// This removes the s-link from the current position in the hierarchy
	public void RemoveSLink() {
		
		if(embed_node == null) {
			return;
		}
		
		if(prev_ptr == null) {
			embed_node.s_links = next_ptr;
		} else {
			prev_ptr.next_ptr = next_ptr;
		}
		
		if(next_ptr != null) {
			next_ptr.prev_ptr = prev_ptr;
		}
		
		embed_node = null;
	}
	
	// This adds a link to its relevant position in the policy node
	public boolean AddLink(PolicyNode node, int max_link) {
		
		if(embed_node != null) {
			RemoveSLink();
		}
		
		embed_node = node;
		int link_num = 0;
		SLink ptr = node.s_links;
		SLink prev = null;
		
		ptr = node.s_links;
		while(ptr != null && compare(ptr, this) < 0) {
			
			if(++link_num >= max_link && max_link >= 0) {
				return false;
			}
			prev = ptr;
			ptr = ptr.next_ptr;
		}
		
		SLink next = ptr;
		
		if(prev != null) {
			prev.next_ptr = this;
		} else {
			node.s_links = this;
		}
		
		this.next_ptr = next;
		
		if(next != null) {
			next.prev_ptr = this;
		}
	
		this.prev_ptr = prev;
		
		if(max_link < 0) {
			return true;
		}
		
		ptr = node.s_links;
		for(int i=0; i<max_link; i++) {
			if(ptr == null) {
				break;
			}
			ptr = ptr.next_ptr;
		}
		
		while(ptr != null) {
			ptr.next_ptr = null;
			ptr.prev_ptr = null;
			ptr.embed_node = null;
			ptr = ptr.next_ptr;
		}
		
		/*int count = 0;
		ptr = node.s_links;
		System.out.println("*************** "+max_link);
		while(ptr != null) {
			System.out.println(ptr.exp_rew+" "+ptr.update_id);
			ptr = ptr.next_ptr;
			count++;
		}
		
		if(count == 0 || count > max_link + 1) {
			System.out.println("error");System.exit(0);
		}*/
		
		return true;
	}
	
	public int compare(SLink arg1, SLink arg2) {
    	
    	if(arg1.update_id < arg2.update_id) {
			return 1;
		}

		if(arg1.update_id > arg2.update_id) {
			return -1;
		}
		
		if(arg1.exp_rew < arg2.exp_rew) {
			return 1;
		}

		if(arg1.exp_rew > arg2.exp_rew) {
			return -1;
		}

		return 0; 
    }

	public int compareTo(SLink arg0) {
		return compare(this, arg0);
	}
}
