// Copyright (c) 2006, Google Inc.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// process_state.cc: A snapshot of a process, in a fully-digested state.
//
// See process_state.h for documentation.
//
// Author: Mark Mentovai
#include "stdio.h"
#include "google_breakpad/processor/process_state.h"
#include "google_breakpad/processor/call_stack.h"
#include "google_breakpad/processor/code_modules.h"
#include "google_breakpad/processor/stack_frame_cpu.h"
#include "processor/pathname_stripper.h"

namespace google_breakpad {

ProcessState::~ProcessState() {
  Clear();
}

void ProcessState::Clear() {
  time_date_stamp_ = 0;
  process_create_time_ = 0;
  crashed_ = false;
  crash_reason_.clear();
  crash_address_ = 0;
  assertion_.clear();
  requesting_thread_ = -1;
  for (vector<CallStack *>::const_iterator iterator = threads_.begin();
       iterator != threads_.end();
       ++iterator) {
    delete *iterator;
  }
  threads_.clear();
  system_info_.Clear();
  // modules_without_symbols_ and modules_with_corrupt_symbols_ DO NOT own
  // the underlying CodeModule pointers.  Just clear the vectors.
  modules_without_symbols_.clear();
  modules_with_corrupt_symbols_.clear();
  delete modules_;
  modules_ = NULL;
}

	void ProcessState::PrintThreadStack(int thread_index, std::string const& cpu) const
	{
		CallStack* stack = threads()->at(thread_index);
		
		printf("\nStack\n");
		MemoryRegion* memory = thread_memory_regions()->at(thread_index);
		//FIXME: take it as x86 for the moment
		uint32_t addr = memory->GetBase();
		uint32_t value = 0;
		for (uint32_t index = 0; index < memory->GetSize(); index +=sizeof(uint32_t), addr += sizeof(uint32_t)) {
			memory->GetMemoryAtAddress(addr, &value);
			printf("%08x %08x", addr, value);
			
			for (unsigned int frame_index = 0; frame_index < stack->frames()->size(); frame_index ++) {
				const StackFrameX86 *frame = reinterpret_cast<const StackFrameX86*>(stack->frames()->at(frame_index));
				if (frame)
				{
					if (addr == frame->context.esp)
						printf("\t<- ESP of frame %d", frame_index);
					
					if (addr == frame->context.ebp)
						printf("\t<- EBP of frame %d", frame_index);
					
					uint32_t instruction_address = (uint32_t)frame->ReturnAddress();
					if (value == instruction_address) {
						if (frame->module) {
							printf("\t%s", PathnameStripper::File(frame->module->code_file()).c_str());
							if (!frame->function_name.empty()) {
								printf("!%s", frame->function_name.c_str());
								if (!frame->source_file_name.empty()) {
									string source_file = PathnameStripper::File(frame->source_file_name);
									printf(" [%s : %d + 0x%x]",
										 source_file.c_str(),
										 frame->source_line,
										 uint32_t(instruction_address - frame->source_line_base));
								} else {
									printf(" + 0x%x", uint32_t(instruction_address - frame->function_base));
								}
							} else {
								printf(" + 0x%x", uint32_t(instruction_address - frame->module->base_address()));
							}
						} else {
							printf("\t0x%x", instruction_address);
						}
						
						break;	// once one is found, print the function name then break the loop
					}
				}
			}
			
			printf("\n");
		}
	}

}  // namespace google_breakpad
